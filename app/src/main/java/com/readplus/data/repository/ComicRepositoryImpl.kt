package com.readplus.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.readplus.data.local.dao.ComicDao
import com.readplus.data.local.entity.ComicCategoryCrossRef
import com.readplus.data.local.entity.ComicEntity
import com.readplus.data.source.ZipArchiveManager
import com.readplus.domain.model.Comic
import com.readplus.domain.repository.ComicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

        // 2. 强制校验 .zip 后缀（防止用户误选图片/APK 等）
        val extension = rawName.substringAfterLast('.', "").lowercase()
        if (extension != "zip") return null

        val title = rawName.substringBeforeLast('.', rawName).takeIf { it.isNotBlank() }
            ?: "未命名漫画"

        // 3. 插入占位实体获得 id
        val placeholder = ComicEntity(
            title = title,
            originalUri = uri.toString(),
            zipPath = "",
            pageCount = 0,
            coverPath = null
        )
        val comicId = dao.insert(placeholder)

        // 4. 复制 ZIP 到 filesDir（失败则回滚）
        val zipPath = runCatching {
            ZipArchiveManager.importZip(context, uri, comicId)
        }.getOrElse {
            dao.findById(comicId)?.let { dao.delete(it) }
            return null
        }

        // 5. 尝试解析 ZIP（非 ZIP 内容会抛 ZipException，必须捕获）
        val entries = runCatching {
            ZipArchiveManager.listImageEntries(zipPath)
        }.getOrElse {
            // 回滚：删除数据库记录 + 已复制的文件
            dao.findById(comicId)?.let { dao.delete(it) }
            runCatching { File(zipPath).delete() }
            return null
        }

        if (entries.isEmpty()) {
            dao.findById(comicId)?.let { dao.delete(it) }
            runCatching { File(zipPath).delete() }
            return null
        }

        // 6. 提取封面
        val coverPath = runCatching {
            ZipArchiveManager.extractCover(
                context, zipPath, entries.first().entryPath, comicId
            )
        }.getOrNull()

        // 7. 更新实体
        dao.findById(comicId)?.let { current ->
            dao.update(
                current.copy(
                    zipPath = zipPath,
                    pageCount = entries.size,
                    coverPath = coverPath
                )
            )
        }
        return comicId
    }

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

    override suspend fun addToCategory(comicId: Long, categoryId: Long) =
        dao.addToCategory(ComicCategoryCrossRef(comicId, categoryId))

    override suspend fun removeFromCategory(comicId: Long, categoryId: Long) =
        dao.removeFromCategory(comicId, categoryId)

    private fun queryDisplayName(uri: Uri): String? =
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }
        }.getOrNull()
}