package com.readplus.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.readplus.data.local.dao.ComicDao
import com.readplus.data.local.entity.ComicCategoryCrossRef
import com.readplus.data.local.entity.ComicEntity
import com.readplus.data.source.CbrExtractor
import com.readplus.data.source.MobiExtractor
import com.readplus.data.source.PdfArchiveManager
import com.readplus.data.source.ZipArchiveManager
import com.readplus.data.source.ZipImageEntry
import com.readplus.domain.model.Comic
import com.readplus.domain.repository.ComicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicRepositoryImpl @Inject constructor(
    private val dao: ComicDao,
    @ApplicationContext private val context: Context
) : ComicRepository {

    private fun toModel(e: ComicEntity) = Comic(
        id = e.id,
        title = e.title,
        zipPath = e.zipPath,
        pageCount = e.pageCount,
        coverPath = e.coverPath,
        lastReadPage = e.lastReadPage,
        lastReadAt = e.lastReadAt
    )

    override fun observeAll(): Flow<List<Comic>> =
        dao.observeAll().map { list -> list.map(::toModel) }

    override fun observeByCategory(categoryId: Long): Flow<List<Comic>> =
        dao.observeByCategory(categoryId).map { list -> list.map(::toModel) }

    override fun observeById(id: Long): Flow<Comic?> =
        dao.observeById(id).map { it?.let(::toModel) }

    override suspend fun getById(id: Long): Comic? = dao.findById(id)?.let(::toModel)

    override suspend fun importZip(uri: Uri): Long? {
        // 1. 读取文件名
        val rawName = queryDisplayName(uri) ?: uri.lastPathSegment?.substringAfterLast('/')
        ?: return null

        // 2. 判断格式
        val extension = rawName.substringAfterLast('.', "").lowercase()
        val isPdf = extension == "pdf"
        val isMobi = extension == "mobi" || extension == "azw" || extension == "azw3"
        val isEpub = extension == "epub"
        val isCbr = extension == "cbr"
        // CBZ 和 ZIP 完全一样，只是扩展名不同
        val isZip = extension == "zip" || extension == "cbz"
        if (!isPdf && !isMobi && !isEpub && !isCbr && !isZip) return null

        val title = rawName.substringBeforeLast('.', rawName).takeIf { it.isNotBlank() }
            ?: "未命名漫画"

        // 3. 插入占位实体
        val placeholder = ComicEntity(
            title = title,
            originalUri = uri.toString(),
            zipPath = "",
            pageCount = 0,
            coverPath = null
        )
        val comicId = dao.insert(placeholder)

        // 4. 本地化
        val localPath: String? = when {
            isPdf -> PdfArchiveManager.importPdf(context, uri, comicId)

            isMobi -> {
                val zipFile = File(ZipArchiveManager.zipCacheDir(context), "comic_$comicId.zip")
                val count = MobiExtractor.extractToZip(context, uri, zipFile)
                if (count > 0) zipFile.absolutePath else null
            }

            isEpub -> runCatching {
                // EPUB 保留为 .epub，listImageEntries 会走 EPUB 分支
                ZipArchiveManager.importZip(context, uri, comicId, extension = "epub")
            }.getOrNull()

            isCbr -> {
                // RAR 提取成 ZIP
                val zipFile = File(ZipArchiveManager.zipCacheDir(context), "comic_$comicId.zip")
                val count = CbrExtractor.extractToZip(context, uri, zipFile)
                if (count > 0) zipFile.absolutePath else null
            }

            else -> runCatching {
                // ZIP / CBZ 统一存成 .zip
                ZipArchiveManager.importZip(context, uri, comicId, extension = "zip")
            }.getOrNull()
        }

        if (localPath == null) {
            dao.findById(comicId)?.let { dao.delete(it) }
            return null
        }

        // 5. 解析页列表
        val entries: List<ZipImageEntry> = if (isPdf) {
            val count = PdfArchiveManager.getPageCount(localPath)
            if (count <= 0) {
                dao.findById(comicId)?.let { dao.delete(it) }
                runCatching { File(localPath).delete() }
                return null
            }
            (0 until count).map {
                ZipImageEntry(name = "page_${it + 1}", entryPath = "$it")
            }
        } else {
            val list = runCatching {
                ZipArchiveManager.listImageEntries(localPath)
            }.getOrNull()
            if (list.isNullOrEmpty()) {
                dao.findById(comicId)?.let { dao.delete(it) }
                runCatching { File(localPath).delete() }
                return null
            }
            list
        }

        // 6. 提取封面
        val coverPath: String? = if (isPdf) {
            PdfArchiveManager.extractCover(context, localPath, comicId)
        } else {
            runCatching {
                ZipArchiveManager.extractCover(
                    context, localPath, entries.first().entryPath, comicId
                )
            }.getOrNull()
        }

        // 7. 更新实体
        dao.findById(comicId)?.let { current ->
            dao.update(
                current.copy(
                    zipPath = localPath,
                    pageCount = entries.size,
                    coverPath = coverPath
                )
            )
        }
        return comicId
    }

    // ============================================================
    // 批量导入
    // ============================================================

    override suspend fun importMultiple(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var count = 0
        uris.forEach { uri ->
            runCatching { importZip(uri) }
                .getOrNull()
                ?.let { count++ }
        }
        count
    }

    override suspend fun importFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext 0

        // 递归扫描所有漫画文件
        val files = mutableListOf<Uri>()
        scanComicFiles(root, files)

        var count = 0
        files.forEach { uri ->
            runCatching { importZip(uri) }
                .getOrNull()
                ?.let { count++ }
        }
        count
    }

    /** 递归扫描目录下所有漫画文件（ZIP/CBZ/CBR/PDF/MOBI/EPUB 等） */
    private fun scanComicFiles(dir: DocumentFile, out: MutableList<Uri>) {
        dir.listFiles().forEach { f ->
            if (f.isDirectory) {
                scanComicFiles(f, out)
            } else {
                val name = f.name ?: return@forEach
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext in COMIC_EXTENSIONS) {
                    out.add(f.uri)
                }
            }
        }
    }

    // ============================================================

    override suspend fun updateProgress(comicId: Long, page: Int) {
        dao.updateProgress(comicId, page)
    }

    override suspend fun delete(comic: Comic) {
        dao.findById(comic.id)?.let {
            runCatching { File(it.zipPath).delete() }
            it.coverPath?.let { p -> runCatching { File(p).delete() } }
            dao.delete(it)
        }
    }

    override suspend fun rename(comicId: Long, newTitle: String) {
        val entity = dao.findById(comicId) ?: return
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty() || trimmed == entity.title) return
        dao.update(entity.copy(title = trimmed))
    }

    override suspend fun addToCategory(comicId: Long, categoryId: Long) =
        dao.addToCategory(ComicCategoryCrossRef(comicId, categoryId))

    override suspend fun removeFromCategory(comicId: Long, categoryId: Long) =
        dao.removeFromCategory(comicId, categoryId)

    // ============================================================
    // 更换封面
    // ============================================================

    override suspend fun setCoverFromPage(comicId: Long, pageIndex: Int): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val entity = dao.findById(comicId) ?: return@runCatching false
                val bytes: ByteArray = if (entity.zipPath.endsWith(".pdf", ignoreCase = true)) {
                    PdfArchiveManager.renderPage(entity.zipPath, pageIndex)
                        ?: return@runCatching false
                } else {
                    val entries = ZipArchiveManager.listImageEntries(entity.zipPath)
                    val entry = entries.getOrNull(pageIndex)
                        ?: return@runCatching false
                    ZipArchiveManager.readBytes(entity.zipPath, entry.entryPath)
                        ?: return@runCatching false
                }

                val newPath = writeCoverFile(bytes, comicId)
                    ?: return@runCatching false

                entity.coverPath?.let { old ->
                    runCatching { File(old).delete() }
                }
                dao.update(entity.copy(coverPath = newPath))
                true
            }.getOrDefault(false)
        }

    override suspend fun setCoverFromUri(comicId: Long, uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val entity = dao.findById(comicId) ?: return@runCatching false
                val bytes = context.contentResolver.openInputStream(uri)?.use {
                    it.readBytes()
                } ?: return@runCatching false

                val newPath = writeCoverFile(bytes, comicId)
                    ?: return@runCatching false

                entity.coverPath?.let { old ->
                    runCatching { File(old).delete() }
                }
                dao.update(entity.copy(coverPath = newPath))
                true
            }.getOrDefault(false)
        }

    private fun writeCoverFile(bytes: ByteArray, comicId: Long): String? =
        runCatching {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@runCatching null
            val dir = ZipArchiveManager.coverDir(context)
            val fileName = "comic_${comicId}_${System.currentTimeMillis()}.jpg"
            val file = File(dir, fileName)
            file.outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it)
            }
            bitmap.recycle()
            file.absolutePath
        }.getOrNull()

    private fun queryDisplayName(uri: Uri): String? =
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }
        }.getOrNull()

    companion object {
        /** 扫描漫画文件夹时识别的扩展名（含 CBZ / CBR） */
        private val COMIC_EXTENSIONS = setOf(
            "zip", "cbz", "cbr", "pdf",
            "mobi", "azw", "azw3", "epub"
        )
    }
}