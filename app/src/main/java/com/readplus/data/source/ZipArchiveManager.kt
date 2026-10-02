package com.readplus.data.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object ZipArchiveManager {

    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif")

    private val zipCache = mutableMapOf<String, ZipFile>()
    // 关键优化：缓存 entry name → ZipEntry 的 map，把 getEntry 从 O(n) 变成 O(1)
    private val entryCache = mutableMapOf<String, Map<String, ZipEntry>>()

    private fun isImage(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS

    fun zipCacheDir(context: Context): File =
        File(context.filesDir, "zip_cache").apply { mkdirs() }

    fun coverDir(context: Context): File =
        File(context.filesDir, "covers").apply { mkdirs() }

    suspend fun importZip(context: Context, uri: Uri, comicId: Long): String =
        withContext(Dispatchers.IO) {
            val dest = File(zipCacheDir(context), "comic_$comicId.zip")
            if (!dest.exists()) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output ->
                        input.copyTo(output, 64 * 1024)
                    }
                } ?: error("无法打开 ZIP: $uri")
            }
            dest.absolutePath
        }

    @Synchronized
    private fun open(zipPath: String): ZipFile =
        zipCache[zipPath] ?: ZipFile(zipPath).also { zipCache[zipPath] = it }

    /** 建立（或复用）entry 索引，只遍历一次 ZIP */
    @Synchronized
    private fun entriesOf(zipPath: String): Map<String, ZipEntry> {
        entryCache[zipPath]?.let { return it }
        val zf = open(zipPath)
        val map = zf.entries().asSequence().associateBy { it.name }
        entryCache[zipPath] = map
        return map
    }

    suspend fun listImageEntries(zipPath: String): List<ZipImageEntry> =
        withContext(Dispatchers.IO) {
            entriesOf(zipPath).values.asSequence()
                .filterNot { it.isDirectory }
                .filter { isImage(it.name) }
                .map {
                    ZipImageEntry(
                        name = it.name.substringAfterLast('/'),
                        entryPath = it.name
                    )
                }
                .sortedWith { a, b -> NaturalOrderComparator().compare(a.name, b.name) }
                .toList()
        }

    suspend fun listImageEntriesFromStream(context: Context, uri: Uri): List<ZipImageEntry> =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    generateSequence { zis.nextEntry }
                        .filterNot { it.isDirectory }
                        .filter { isImage(it.name) }
                        .map {
                            ZipImageEntry(it.name.substringAfterLast('/'), it.name)
                        }
                        .toList()
                        .sortedWith { a, b -> NaturalOrderComparator().compare(a.name, b.name) }
                }
            } ?: emptyList()
        }

    suspend fun readBytes(zipPath: String, entryPath: String): ByteArray? =
        withContext(Dispatchers.IO) {
            runCatching {
                val zf = open(zipPath)
                // 用缓存的 map 直接拿 entry，不再遍历整个 ZIP
                val entry = entriesOf(zipPath)[entryPath] ?: return@runCatching null
                zf.getInputStream(entry).use { it.readBytes() }
            }.getOrNull()
        }

    suspend fun extractCover(
        context: Context,
        zipPath: String,
        entryPath: String,
        comicId: Long
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = readBytes(zipPath, entryPath) ?: return@runCatching null
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@runCatching null
            val coverFile = File(coverDir(context), "comic_$comicId.jpg")
            coverFile.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle()
            coverFile.absolutePath
        }.getOrNull()
    }

    @Synchronized
    fun closeAll() {
        zipCache.values.forEach { runCatching { it.close() } }
        zipCache.clear()
        entryCache.clear()
    }
}