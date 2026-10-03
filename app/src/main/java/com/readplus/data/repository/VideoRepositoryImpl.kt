package com.readplus.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.readplus.data.local.dao.VideoDao
import com.readplus.data.local.entity.VideoCategoryCrossRef
import com.readplus.data.local.entity.VideoEntity
import com.readplus.data.source.VideoImporter
import com.readplus.domain.model.Video
import com.readplus.domain.repository.VideoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepositoryImpl @Inject constructor(
    private val dao: VideoDao,
    @ApplicationContext private val context: Context
) : VideoRepository {

    private fun toModel(e: VideoEntity) = Video(
        id = e.id,
        title = e.title,
        uri = e.uri,
        duration = e.duration,
        coverPath = e.coverPath
    )

    override fun observeAll(): Flow<List<Video>> =
        dao.observeAll().map { it.map(::toModel) }

    override fun observeByCategory(categoryId: Long): Flow<List<Video>> =
        dao.observeByCategory(categoryId).map { it.map(::toModel) }

    override suspend fun importMultiple(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var count = 0
        uris.forEach { uri ->
            val entity = VideoImporter.buildEntity(context, uri) ?: return@forEach
            dao.insert(entity)
            count++
        }
        count
    }

    override suspend fun importFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext 0
        val entities = mutableListOf<VideoEntity>()
        scan(root, treeUri, entities)
        var count = 0
        entities.forEach {
            dao.insert(it)
            count++
        }
        count
    }

    private fun scan(dir: DocumentFile, rootUri: Uri, out: MutableList<VideoEntity>) {
        dir.listFiles().forEach { f ->
            if (f.isDirectory) {
                scan(f, rootUri, out)
            } else {
                val name = f.name ?: return@forEach
                if (!VideoImporter.isVideoFileName(name)) return@forEach
                out.add(
                    VideoEntity(
                        // 去掉扩展名作为显示标题
                        title = name.substringBeforeLast('.', name),
                        uri = f.uri.toString(),
                        size = f.length(),
                        coverPath = null,
                        folderUri = rootUri.toString(),
                        importSource = "FOLDER"
                    )
                )
            }
        }
    }

    override suspend fun delete(video: Video) {
        dao.findById(video.id)?.let {
            it.coverPath?.let { p -> runCatching { java.io.File(p).delete() } }
            dao.delete(it)
        }
    }

    override suspend fun rename(videoId: Long, newTitle: String) {
        val entity = dao.findById(videoId) ?: return
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty() || trimmed == entity.title) return
        dao.update(entity.copy(title = trimmed))
    }

    override suspend fun addToCategory(videoId: Long, categoryId: Long) =
        dao.addToCategory(VideoCategoryCrossRef(videoId, categoryId))

    override suspend fun removeFromCategory(videoId: Long, categoryId: Long) =
        dao.removeFromCategory(videoId, categoryId)

    override suspend fun getCategoryNamesByVideo(): Map<Long, List<String>> =
        withContext(Dispatchers.IO) {
            dao.getAllVideoCategoryNames()
                .groupBy({ it.videoId }, { it.categoryName })
        }
}