package com.readplus.data.source

import android.content.Context
import android.net.Uri
import com.github.junrar.Archive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * CBR（RAR 格式漫画）提取器。
 *
 * 策略：导入时把 RAR 内容解压成标准 ZIP 缓存到 filesDir/zip_cache/。
 * 之后的阅读、缩略图、翻页等逻辑完全复用 ZIP 流程。
 *
 * 支持的格式：
 * - RAR4（老格式，junrar 完全支持）
 * - RAR5（新格式，junrar 部分支持；复杂特性可能失败）
 *
 * 图片顺序：按文件名自然排序，与 CBZ / ZIP 漫画保持一致。
 */
object CbrExtractor {

    private val IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "bmp"
    )

    private fun isImage(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS

    /**
     * 从 CBR 文件提取所有图片并写入目标 ZIP。
     * @return 提取到的图片数量；0 表示失败
     */
    suspend fun extractToZip(
        context: Context,
        sourceUri: Uri,
        targetZipFile: File
    ): Int = withContext(Dispatchers.IO) {
        // RAR 库需要真实文件路径，先把 URI 内容复制到临时文件
        val tempRar = File(
            context.cacheDir,
            "temp_cbr_${System.currentTimeMillis()}.rar"
        )

        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                tempRar.outputStream().use { output ->
                    input.copyTo(output, 64 * 1024)
                }
            } ?: return@withContext 0

            val images = mutableListOf<Pair<String, ByteArray>>()

            Archive(tempRar).use { archive ->
                archive.fileHeaders?.forEach { header ->
                    if (header == null || header.isDirectory) return@forEach
                    val name = header.fileName ?: return@forEach
                    if (!isImage(name)) return@forEach

                    runCatching {
                        val bytes = archive.getInputStream(header)?.use {
                            it.readBytes()
                        } ?: return@runCatching
                        if (bytes.isNotEmpty()) {
                            images.add(name to bytes)
                        }
                    }
                }
            }

            if (images.isEmpty()) return@withContext 0

            // 按文件名自然排序（与 ZIP / CBZ 一致）
            val comparator = NaturalOrderComparator()
            val sorted = images.sortedWith { a, b ->
                comparator.compare(
                    a.first.substringAfterLast('/'),
                    b.first.substringAfterLast('/')
                )
            }

            // 写入目标 ZIP，文件名统一为 page_0001.ext
            targetZipFile.parentFile?.mkdirs()
            ZipOutputStream(targetZipFile.outputStream().buffered()).use { zos ->
                sorted.forEachIndexed { index, (originalName, bytes) ->
                    val ext = originalName.substringAfterLast('.', "jpg")
                    val entryName = "page_${(index + 1).toString().padStart(4, '0')}.$ext"
                    zos.putNextEntry(ZipEntry(entryName))
                    zos.write(bytes)
                    zos.closeEntry()
                }
            }
            sorted.size
        } catch (e: Exception) {
            0
        } finally {
            runCatching { tempRar.delete() }
        }
    }
}