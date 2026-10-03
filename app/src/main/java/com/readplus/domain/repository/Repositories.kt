package com.readplus.domain.repository

import android.net.Uri
import com.readplus.domain.model.Category
import com.readplus.domain.model.Comic
import com.readplus.domain.model.Video
import kotlinx.coroutines.flow.Flow

interface ComicRepository {
    fun observeAll(): Flow<List<Comic>>
    fun observeByCategory(categoryId: Long): Flow<List<Comic>>
    fun observeById(id: Long): Flow<Comic?>
    suspend fun getById(id: Long): Comic?
    suspend fun importZip(uri: Uri): Long?

    /** 批量导入多个漫画文件，返回成功导入的数量 */
    suspend fun importMultiple(uris: List<Uri>): Int

    /** 递归扫描文件夹，导入其中所有漫画文件，返回成功导入的数量 */
    suspend fun importFolder(treeUri: Uri): Int

    suspend fun updateProgress(comicId: Long, page: Int)
    suspend fun delete(comic: Comic)

    /** 重命名漫画 */
    suspend fun rename(comicId: Long, newTitle: String)

    suspend fun addToCategory(comicId: Long, categoryId: Long)
    suspend fun removeFromCategory(comicId: Long, categoryId: Long)

    /** 从漫画的某一页（0-based）提取封面 */
    suspend fun setCoverFromPage(comicId: Long, pageIndex: Int): Boolean

    /** 从外部图片设置封面 */
    suspend fun setCoverFromUri(comicId: Long, uri: Uri): Boolean
}

interface VideoRepository {
    fun observeAll(): Flow<List<Video>>
    fun observeByCategory(categoryId: Long): Flow<List<Video>>
    suspend fun importMultiple(uris: List<Uri>): Int
    suspend fun importFolder(treeUri: Uri): Int
    suspend fun delete(video: Video)

    /** 重命名视频 */
    suspend fun rename(videoId: Long, newTitle: String)

    suspend fun addToCategory(videoId: Long, categoryId: Long)
    suspend fun removeFromCategory(videoId: Long, categoryId: Long)
    suspend fun getCategoryNamesByVideo(): Map<Long, List<String>>
}

interface CategoryRepository {
    fun observeByType(type: String): Flow<List<Category>>
    suspend fun listByType(type: String): List<Category>
    suspend fun create(name: String, type: String): Long
    suspend fun rename(id: Long, newName: String)
    suspend fun delete(id: Long)
    suspend fun ensureManualImportCategory(): Long
}