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

    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

    private val zipCache = mutableMapOf<String, ZipFile>()
    private val entryCache = mutableMapOf<String, Map<String, ZipEntry>>()

    private fun isImage(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS

    private fun isPdf(path: String): Boolean =
        path.endsWith(".pdf", ignoreCase = true)

    private fun isEpub(path: String): Boolean =
        path.endsWith(".epub", ignoreCase = true)

    fun zipCacheDir(context: Context): File =
        File(context.filesDir, "zip_cache").apply { mkdirs() }

    fun coverDir(context: Context): File =
        File(context.filesDir, "covers").apply { mkdirs() }

    /**
     * 复制文件到 filesDir/zip_cache/。
     * @param extension 目标文件扩展名（"zip" / "epub"）
     */
    suspend fun importZip(
        context: Context,
        uri: Uri,
        comicId: Long,
        extension: String = "zip"
    ): String = withContext(Dispatchers.IO) {
        val dest = File(zipCacheDir(context), "comic_$comicId.$extension")
        if (!dest.exists() || dest.length() == 0L) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output, 64 * 1024)
                }
            } ?: error("无法打开文件: $uri")
        }
        dest.absolutePath
    }

    @Synchronized
    private fun open(zipPath: String): ZipFile =
        zipCache[zipPath] ?: ZipFile(zipPath).also { zipCache[zipPath] = it }

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
            when {
                // 1. PDF
                isPdf(zipPath) -> {
                    val count = PdfArchiveManager.getPageCount(zipPath)
                    (0 until count).map {
                        ZipImageEntry(name = "page_${it + 1}", entryPath = "$it")
                    }
                }
                // 2. EPUB：按 OPF spine 顺序
                isEpub(zipPath) -> {
                    val order = EpubArchiveManager.parseContentOrder(zipPath)
                    if (!order.isNullOrEmpty()) {
                        val entries = entriesOf(zipPath)
                        order.mapIndexedNotNull { index, path ->
                            if (entries.containsKey(path)) {
                                ZipImageEntry(
                                    name = "page_${(index + 1).toString().padStart(4, '0')}",
                                    entryPath = path
                                )
                            } else null
                        }.takeIf { it.isNotEmpty() } ?: fallbackZipEntries(zipPath)
                    } else {
                        fallbackZipEntries(zipPath)
                    }
                }
                // 3. ZIP / MOBI 提取后的 ZIP
                else -> fallbackZipEntries(zipPath)
            }
        }

    /** 按文件名自然排序 */
    private fun fallbackZipEntries(zipPath: String): List<ZipImageEntry> =
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