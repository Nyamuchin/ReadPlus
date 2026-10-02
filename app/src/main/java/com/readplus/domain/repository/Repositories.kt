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
    suspend fun updateProgress(comicId: Long, page: Int)
    suspend fun delete(comic: Comic)
    suspend fun addToCategory(comicId: Long, categoryId: Long)
    suspend fun removeFromCategory(comicId: Long, categoryId: Long)
}

interface VideoRepository {
    fun observeAll(): Flow<List<Video>>
    fun observeByCategory(categoryId: Long): Flow<List<Video>>
    suspend fun importMultiple(uris: List<Uri>): Int
    suspend fun importFolder(treeUri: Uri): Int
    suspend fun delete(video: Video)
    suspend fun addToCategory(videoId: Long, categoryId: Long)
    suspend fun removeFromCategory(videoId: Long, categoryId: Long)
    /** 获取 videoId → 分类名列表 的映射 */
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