package com.readplus.data.source

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.readplus.data.local.entity.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object VideoImporter {

    private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm", "avi")

    fun isVideoFileName(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS

    fun coverDir(context: Context): File =
        File(context.filesDir, "video_covers").apply { mkdirs() }

    suspend fun buildEntity(context: Context, uri: Uri): VideoEntity? =
        withContext(Dispatchers.IO) {
            val name = queryDisplayName(context, uri) ?: return@withContext null
            if (!isVideoFileName(name)) return@withContext null

            val size = querySize(context, uri)
            val (duration, coverPath) = readMetaAndCover(context, uri, name)
            // 去掉扩展名作为显示标题
            val title = name.substringBeforeLast('.', name)

            VideoEntity(
                title = title,
                uri = uri.toString(),
                duration = duration,
                size = size,
                coverPath = coverPath,
                folderUri = null,
                importSource = "MANUAL"
            )
        }

    private fun queryDisplayName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }

    private fun querySize(context: Context, uri: Uri): Long =
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.SIZE)
            if (idx >= 0 && c.moveToFirst()) c.getLong(idx) else 0L
        } ?: 0L

    private fun readMetaAndCover(
        context: Context,
        uri: Uri,
        fileName: String
    ): Pair<Long, String?> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val duration = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L

            val frame = retriever.getFrameAtTime(
                0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            val coverPath = frame?.let { bmp ->
                val scaled = Bitmap.createScaledBitmap(
                    bmp,
                    (bmp.width * 0.5f).toInt().coerceAtLeast(320),
                    (bmp.height * 0.5f).toInt().coerceAtLeast(180),
                    true
                )
                val file = File(
                    coverDir(context),
                    "video_${fileName.hashCode()}.jpg"
                )
                file.outputStream().use {
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, it)
                }
                bmp.recycle()
                if (scaled !== bmp) scaled.recycle()
                file.absolutePath
            }
            duration to coverPath
        } catch (e: Exception) {
            0L to null
        } finally {
            runCatching { retriever.release() }
        }
    }
}