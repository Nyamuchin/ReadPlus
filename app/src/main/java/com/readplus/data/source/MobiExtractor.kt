package com.readplus.data.source

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * MOBI 图片提取器。
 *
 * 策略：不完整解析 MOBI 结构，直接扫描字节流找图片头。
 * MOBI 中的图片以独立资源存储，JPEG/PNG/GIF/WebP 都有固定文件头，
 * 我们扫描整个文件，依次提取所有图片，然后打包成标准 ZIP。
 *
 * 提取出的 ZIP 与普通漫画 ZIP 完全一致，可复用现有全部阅读流程。
 *
 * 局限：
 * - HUFF/CDIC 压缩的 MOBI 可能提取失败（返回 0）
 * - 带 DRM 的 MOBI 无法提取（返回 0）
 * - 图片顺序按文件中出现顺序，绝大多数漫画 MOBI 与阅读顺序一致
 */
object MobiExtractor {

    private val JPEG_SOI = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val PNG_SIG = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )
    private val GIF_SIG = byteArrayOf(0x47, 0x49, 0x46, 0x38) // "GIF8"
    private val WEBP_RIFF = byteArrayOf(0x52, 0x49, 0x46, 0x46) // "RIFF"
    private val WEBP_MARK = byteArrayOf(0x57, 0x45, 0x42, 0x50) // "WEBP"

    // 最小图片大小（字节），过滤误识别的碎片
    private const val MIN_IMAGE_SIZE = 1024

    /**
     * 从 MOBI 文件提取所有图片，写入目标 ZIP。
     * @return 提取到的图片数量；0 表示失败
     */
    suspend fun extractToZip(
        context: Context,
        sourceUri: Uri,
        targetZipFile: File
    ): Int = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(sourceUri)
                ?.use { it.readBytes() }
                ?: return@runCatching 0

            if (bytes.size < 1024) return@runCatching 0

            val images = extractImagesFromBytes(bytes)
            if (images.isEmpty()) return@runCatching 0

            // 写 ZIP
            targetZipFile.parentFile?.mkdirs()
            ZipOutputStream(targetZipFile.outputStream().buffered()).use { zos ->
                images.forEachIndexed { index, imgBytes ->
                    val entryName = "page_${(index + 1).toString().padStart(4, '0')}.jpg"
                    zos.putNextEntry(ZipEntry(entryName))
                    zos.write(imgBytes)
                    zos.closeEntry()
                }
            }
            images.size
        }.getOrDefault(0)
    }

    private fun extractImagesFromBytes(data: ByteArray): List<ByteArray> {
        val images = mutableListOf<ByteArray>()
        var i = 0
        val size = data.size

        while (i < size - 12) {
            val imgEnd = when {
                matches(data, i, JPEG_SOI) -> findJpegEnd(data, i)
                matches(data, i, PNG_SIG) -> findPngEnd(data, i)
                matches(data, i, GIF_SIG) -> findGifEnd(data, i)
                matches(data, i, WEBP_RIFF) && matches(data, i + 8, WEBP_MARK) ->
                    findWebpEnd(data, i)
                else -> -1
            }

            if (imgEnd > i && imgEnd - i >= MIN_IMAGE_SIZE) {
                images.add(data.copyOfRange(i, imgEnd))
                i = imgEnd
            } else {
                i++
            }
        }
        return images
    }

    // ---- 各格式的结尾探测 ----

    /** JPEG 结束标记：FF D9 */
    private fun findJpegEnd(data: ByteArray, start: Int): Int {
        var i = start + 3
        val end = data.size
        while (i < end - 1) {
            if (data[i] == 0xFF.toByte() && data[i + 1] == 0xD9.toByte()) {
                return i + 2
            }
            i++
        }
        return -1
    }

    /** PNG 结束：IEND 块（4 字节长度 + "IEND" + 4 字节 CRC） */
    private fun findPngEnd(data: ByteArray, start: Int): Int {
        var i = start + 8
        val end = data.size
        while (i < end - 12) {
            val length = readU32BE(data, i)
            if (length < 0 || length > end - i - 12) return -1
            val type = String(data, i + 4, 4, Charsets.US_ASCII)
            val next = i + 8 + length + 4
            if (type == "IEND") {
                return next
            }
            i = next
        }
        return -1
    }

    /** GIF 结束标记：0x3B */
    private fun findGifEnd(data: ByteArray, start: Int): Int {
        // 简单向后扫描，从 start+6 开始找 0x3B
        // 为了避免把 0x3B 当成普通字节误判，从 start + 100 开始找
        var i = start + 100
        val end = data.size
        while (i < end) {
            if (data[i] == 0x3B.toByte()) {
                return i + 1
            }
            i++
        }
        return -1
    }

    /** WebP：RIFF 头包含总长度 */
    private fun findWebpEnd(data: ByteArray, start: Int): Int {
        if (start + 8 > data.size) return -1
        val riffSize = readU32LE(data, start + 4)
        if (riffSize < 12) return -1
        val end = start + 8 + riffSize
        return if (end <= data.size) end else -1
    }

    // ---- 工具方法 ----

    private fun matches(data: ByteArray, offset: Int, sig: ByteArray): Boolean {
        if (offset < 0 || offset + sig.size > data.size) return false
        for (i in sig.indices) {
            if (data[offset + i] != sig[i]) return false
        }
        return true
    }

    private fun readU32BE(data: ByteArray, offset: Int): Int {
        if (offset + 4 > data.size) return -1
        return ((data[offset].toInt() and 0xFF) shl 24) or
                ((data[offset + 1].toInt() and 0xFF) shl 16) or
                ((data[offset + 2].toInt() and 0xFF) shl 8) or
                (data[offset + 3].toInt() and 0xFF)
    }

    private fun readU32LE(data: ByteArray, offset: Int): Int {
        if (offset + 4 > data.size) return -1
        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                ((data[offset + 3].toInt() and 0xFF) shl 24)
    }
}