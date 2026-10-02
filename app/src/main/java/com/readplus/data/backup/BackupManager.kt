package com.readplus.data.backup

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.gson.GsonBuilder
import com.readplus.data.local.ReadPlusDatabase
import com.readplus.data.local.entity.CategoryEntity
import com.readplus.data.local.entity.ComicCategoryCrossRef
import com.readplus.data.local.entity.ComicEntity
import com.readplus.data.local.entity.VideoCategoryCrossRef
import com.readplus.data.local.entity.VideoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

// ---- 备份数据结构 ----

data class BackupData(
    val version: Int = 2,               // v2 包含媒体文件
    val exportedAt: Long = System.currentTimeMillis(),
    val comics: List<ComicBackup> = emptyList(),
    val videos: List<VideoBackup> = emptyList(),
    val categories: List<CategoryBackup> = emptyList(),
    val comicCategoryRefs: List<ComicCategoryRefBackup> = emptyList(),
    val videoCategoryRefs: List<VideoCategoryRefBackup> = emptyList()
)

data class ComicBackup(
    val id: Long,
    val title: String,
    val originalUri: String,
    val storedZipName: String?,         // ZIP 内文件名（如 comic_1.zip）
    val pageCount: Int,
    val coverFileName: String?,
    val lastReadPage: Int,
    val lastReadAt: Long,
    val createdAt: Long
)

data class VideoBackup(
    val id: Long,
    val title: String,
    val storedFileName: String?,        // ZIP 内文件名（如 video_1.mp4）
    val duration: Long,
    val size: Long,
    val coverFileName: String?,
    val importSource: String,
    val createdAt: Long
)

data class CategoryBackup(
    val id: Long,
    val name: String,
    val type: String,
    val sortOrder: Int,
    val createdAt: Long
)

data class ComicCategoryRefBackup(val comicId: Long, val categoryId: Long)
data class VideoCategoryRefBackup(val videoId: Long, val categoryId: Long)

// ---- 进度回调 ----

fun interface ProgressCallback {
    fun onProgress(phase: String, current: Int, total: Int)
}

// ---- 核心 ----

@Singleton
class BackupManager @Inject constructor(
    private val db: ReadPlusDatabase,
    @ApplicationContext private val context: Context
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * 完整导出（含媒体文件）：
     *   ZIP 结构：
     *   - data.json
     *   - covers/<fileName>.jpg             漫画封面
     *   - video_covers/<fileName>.jpg       视频封面
     *   - zips/comic_<id>.zip               漫画 ZIP 完整文件
     *   - videos/video_<id>.<ext>           视频完整文件
     */
    suspend fun export(
        targetUri: Uri,
        onProgress: ProgressCallback? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val comics = db.comicDao().getAllOnce()
            val videos = db.videoDao().getAllOnce()
            val categories = db.categoryDao().getAllOnce()
            val comicRefs = db.comicDao().getAllRefs()
            val videoRefs = db.videoDao().getAllRefs()

            // 先计算总任务数，用于进度显示
            val totalSteps = comics.size + videos.size
            var step = 0

            // 预扫描：为每个视频解析扩展名 + 原显示名
            val videoFileNames = mutableMapOf<Long, String>()
            videos.forEach { v ->
                val displayName = queryDisplayName(Uri.parse(v.uri))
                val ext = displayName?.substringAfterLast('.', "mp4")?.lowercase() ?: "mp4"
                videoFileNames[v.id] = "video_${v.id}.$ext"
            }

            val outputStream = context.contentResolver.openOutputStream(targetUri)
                ?: error("无法打开输出流")

            ZipOutputStream(BufferedOutputStream(outputStream)).use { zos ->
                // ============ 1. 写入 data.json 占位（先写空的，最后回填也行，
                // 但为了简化，先收集全部元数据，等媒体都处理完再写）
                // 我们这里先处理媒体文件，最后写 data.json
                // 但由于 ZipOutputStream 是顺序写，必须提前想好顺序
                // => 先写 data.json（不带 storedName），然后写媒体，再更新 data.json？不行
                // 解决方案：先预扫描所有媒体，确定哪些能导出，再写

                // 预扫描漫画 ZIP
                val comicZipNames = mutableMapOf<Long, String>()
                comics.forEach { c ->
                    val f = File(c.zipPath)
                    if (f.exists() && f.length() > 0) {
                        comicZipNames[c.id] = "comic_${c.id}.zip"
                    }
                }

                // 预扫描视频源（只记录能打开的）
                val videoSources = mutableMapOf<Long, Pair<Uri, String>>()  // id -> (uri, storedName)
                videos.forEach { v ->
                    val uri = Uri.parse(v.uri)
                    if (canOpenUri(uri)) {
                        videoSources[v.id] = uri to (videoFileNames[v.id] ?: "video_${v.id}.mp4")
                    }
                }

                // 构造 BackupData
                val backup = BackupData(
                    comics = comics.map {
                        ComicBackup(
                            id = it.id,
                            title = it.title,
                            originalUri = it.originalUri,
                            storedZipName = comicZipNames[it.id],
                            pageCount = it.pageCount,
                            coverFileName = it.coverPath?.let { p -> File(p).name },
                            lastReadPage = it.lastReadPage,
                            lastReadAt = it.lastReadAt,
                            createdAt = it.createdAt
                        )
                    },
                    videos = videos.map {
                        VideoBackup(
                            id = it.id,
                            title = it.title,
                            storedFileName = videoSources[it.id]?.second,
                            duration = it.duration,
                            size = it.size,
                            coverFileName = it.coverPath?.let { p -> File(p).name },
                            importSource = it.importSource,
                            createdAt = it.createdAt
                        )
                    },
                    categories = categories.map {
                        CategoryBackup(
                            id = it.id,
                            name = it.name,
                            type = it.type,
                            sortOrder = it.sortOrder,
                            createdAt = it.createdAt
                        )
                    },
                    comicCategoryRefs = comicRefs.map {
                        ComicCategoryRefBackup(it.comicId, it.categoryId)
                    },
                    videoCategoryRefs = videoRefs.map {
                        VideoCategoryRefBackup(it.videoId, it.categoryId)
                    }
                )

                // ============ 写 data.json ============
                zos.putNextEntry(ZipEntry("data.json"))
                zos.write(gson.toJson(backup).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // ============ 写漫画封面 ============
                onProgress?.onProgress("导出漫画封面", 0, comics.size)
                comics.forEachIndexed { i, c ->
                    c.coverPath?.let { p ->
                        val f = File(p)
                        if (f.exists() && f.length() > 0) {
                            zos.putNextEntry(ZipEntry("covers/${f.name}"))
                            f.inputStream().use { it.copyTo(zos, 64 * 1024) }
                            zos.closeEntry()
                        }
                    }
                    onProgress?.onProgress("导出漫画封面", i + 1, comics.size)
                }

                // ============ 写视频封面 ============
                onProgress?.onProgress("导出视频封面", 0, videos.size)
                videos.forEachIndexed { i, v ->
                    v.coverPath?.let { p ->
                        val f = File(p)
                        if (f.exists() && f.length() > 0) {
                            zos.putNextEntry(ZipEntry("video_covers/${f.name}"))
                            f.inputStream().use { it.copyTo(zos, 64 * 1024) }
                            zos.closeEntry()
                        }
                    }
                    onProgress?.onProgress("导出视频封面", i + 1, videos.size)
                }

                // ============ 写漫画 ZIP ============
                onProgress?.onProgress("导出漫画文件", 0, comics.size)
                comics.forEachIndexed { i, c ->
                    val storedName = comicZipNames[c.id]
                    if (storedName != null) {
                        val f = File(c.zipPath)
                        if (f.exists()) {
                            zos.putNextEntry(ZipEntry("zips/$storedName"))
                            f.inputStream().use { it.copyTo(zos, 64 * 1024) }
                            zos.closeEntry()
                        }
                    }
                    step++
                    onProgress?.onProgress("导出漫画文件", i + 1, comics.size)
                }

                // ============ 写视频文件 ============
                onProgress?.onProgress("导出视频文件", 0, videos.size)
                videos.forEachIndexed { i, v ->
                    val src = videoSources[v.id]
                    if (src != null) {
                        val (uri, storedName) = src
                        runCatching {
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                zos.putNextEntry(ZipEntry("videos/$storedName"))
                                input.copyTo(zos, 128 * 1024)
                                zos.closeEntry()
                            }
                        }
                    }
                    onProgress?.onProgress("导出视频文件", i + 1, videos.size)
                }
            }
        }
    }

    /**
     * 完整导入：
     * 1. 解压所有资源到 filesDir 对应目录
     * 2. 清空数据库
     * 3. 恢复所有记录，媒体路径指向新解压的文件
     */
    suspend fun import(
        sourceUri: Uri,
        onProgress: ProgressCallback? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
            val videoCoversDir = File(context.filesDir, "video_covers").apply { mkdirs() }
            val zipsDir = File(context.filesDir, "zip_cache").apply { mkdirs() }
            val videosDir = File(context.filesDir, "videos").apply { mkdirs() }

            // 清空旧媒体文件，避免残留
            zipsDir.listFiles()?.forEach { it.delete() }
            videosDir.listFiles()?.forEach { it.delete() }

            // 解压：先扫一遍统计条目数用于进度
            var backup: BackupData? = null
            var totalEntries = 0

            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: error("无法打开输入流")

            // 第一次扫描：数条目数
            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    totalEntries++
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // 第二次扫描：实际解压
            val inputStream2 = context.contentResolver.openInputStream(sourceUri)
                ?: error("无法打开输入流")
            var current = 0

            ZipInputStream(BufferedInputStream(inputStream2)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    current++
                    onProgress?.onProgress("导入中", current, totalEntries)

                    when {
                        name == "data.json" -> {
                            val text = zis.readBytes().toString(Charsets.UTF_8)
                            backup = gson.fromJson(text, BackupData::class.java)
                        }
                        name.startsWith("covers/") -> {
                            val fileName = name.removePrefix("covers/")
                            if (fileName.isNotBlank()) {
                                File(coversDir, fileName).outputStream().use { zis.copyTo(it, 64 * 1024) }
                            }
                        }
                        name.startsWith("video_covers/") -> {
                            val fileName = name.removePrefix("video_covers/")
                            if (fileName.isNotBlank()) {
                                File(videoCoversDir, fileName).outputStream().use { zis.copyTo(it, 64 * 1024) }
                            }
                        }
                        name.startsWith("zips/") -> {
                            val fileName = name.removePrefix("zips/")
                            if (fileName.isNotBlank()) {
                                File(zipsDir, fileName).outputStream().use { zis.copyTo(it, 128 * 1024) }
                            }
                        }
                        name.startsWith("videos/") -> {
                            val fileName = name.removePrefix("videos/")
                            if (fileName.isNotBlank()) {
                                File(videosDir, fileName).outputStream().use { zis.copyTo(it, 128 * 1024) }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val data = backup ?: error("备份文件缺少 data.json")

            // 清空数据库
            db.clearAllTables()

            // 恢复分类
            onProgress?.onProgress("恢复分类", 0, data.categories.size)
            data.categories.forEachIndexed { i, c ->
                db.categoryDao().insert(
                    CategoryEntity(
                        id = c.id,
                        name = c.name,
                        type = c.type,
                        sortOrder = c.sortOrder,
                        createdAt = c.createdAt
                    )
                )
                onProgress?.onProgress("恢复分类", i + 1, data.categories.size)
            }

            // 恢复漫画
            onProgress?.onProgress("恢复漫画", 0, data.comics.size)
            data.comics.forEachIndexed { i, c ->
                val zipFile = c.storedZipName?.let { File(zipsDir, it) }
                val coverFile = c.coverFileName?.let { File(coversDir, it) }
                db.comicDao().insert(
                    ComicEntity(
                        id = c.id,
                        title = c.title,
                        originalUri = c.originalUri,
                        zipPath = zipFile?.absolutePath ?: "",
                        pageCount = c.pageCount,
                        coverPath = coverFile?.takeIf { it.exists() }?.absolutePath,
                        lastReadPage = c.lastReadPage,
                        lastReadAt = c.lastReadAt,
                        createdAt = c.createdAt
                    )
                )
                onProgress?.onProgress("恢复漫画", i + 1, data.comics.size)
            }

            // 恢复视频
            onProgress?.onProgress("恢复视频", 0, data.videos.size)
            data.videos.forEachIndexed { i, v ->
                val videoFile = v.storedFileName?.let { File(videosDir, it) }
                val coverFile = v.coverFileName?.let { File(videoCoversDir, it) }
                // 视频 URI 改成 file:// 指向解压出来的本地文件
                val newUri = if (videoFile != null && videoFile.exists()) {
                    Uri.fromFile(videoFile).toString()
                } else {
                    "" // 文件不存在（用户备份时文件就已经丢了）
                }
                db.videoDao().insert(
                    VideoEntity(
                        id = v.id,
                        title = v.title,
                        uri = newUri,
                        duration = v.duration,
                        size = v.size,
                        coverPath = coverFile?.takeIf { it.exists() }?.absolutePath,
                        folderUri = null,
                        importSource = v.importSource,
                        createdAt = v.createdAt
                    )
                )
                onProgress?.onProgress("恢复视频", i + 1, data.videos.size)
            }

            // 恢复关联
            data.comicCategoryRefs.forEach { r ->
                db.comicDao().addToCategory(ComicCategoryCrossRef(r.comicId, r.categoryId))
            }
            data.videoCategoryRefs.forEach { r ->
                db.videoDao().addToCategory(VideoCategoryCrossRef(r.videoId, r.categoryId))
            }
        }
    }

    // ---- 工具方法 ----

    private fun queryDisplayName(uri: Uri): String? =
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }
        }.getOrNull()

    private fun canOpenUri(uri: Uri): Boolean =
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        }.getOrDefault(false)
}