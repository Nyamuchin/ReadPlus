package com.readplus.data.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * PDF 文件处理。
 *
 * - 导入时把 PDF 复制到 filesDir/pdf_cache/
 * - 页数：用 PdfRenderer.pageCount
 * - 渲染：把某页渲染成 JPEG 字节返回，供 Coil 使用
 * - 封面：渲染第 0 页保存成 jpg
 *
 * 重要：PdfRenderer 不是线程安全的，且同一时刻只允许一页处于 open 状态。
 * 每次渲染都重新打开 PdfRenderer，虽然有一点开销，但避免了并发问题。
 */
object PdfArchiveManager {

    // 渲染时的最大边长（像素），用来控制内存占用
    private const val MAX_LONG_SIDE = 1600
    private const val MAX_SCALE = 2.0f
    private const val MIN_SCALE = 0.5f

    fun pdfCacheDir(context: Context): File =
        File(context.filesDir, "pdf_cache").apply { mkdirs() }

    fun coverDir(context: Context): File =
        File(context.filesDir, "covers").apply { mkdirs() }

    /** 把 SAF URI 的 PDF 复制到 filesDir，返回本地路径 */
    suspend fun importPdf(context: Context, uri: Uri, comicId: Long): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val dest = File(pdfCacheDir(context), "comic_$comicId.pdf")
                if (!dest.exists() || dest.length() == 0L) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        dest.outputStream().use { output ->
                            input.copyTo(output, 64 * 1024)
                        }
                    } ?: error("无法打开 PDF: $uri")
                }
                dest.absolutePath
            }.getOrNull()
        }

    /** 获取 PDF 页数，失败返回 0 */
    suspend fun getPageCount(pdfPath: String): Int = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(pdfPath)
            if (!file.exists()) return@runCatching 0
            ParcelFileDescriptor
                .open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                .use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        renderer.pageCount
                    }
                }
        }.getOrDefault(0)
    }

    /** 渲染指定页为 JPEG 字节 */
    suspend fun renderPage(pdfPath: String, pageIndex: Int): ByteArray? =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(pdfPath)
                if (!file.exists()) return@runCatching null

                ParcelFileDescriptor
                    .open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    .use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                                return@use null
                            }
                            val page = renderer.openPage(pageIndex)
                            page.use { p ->
                                // 计算渲染尺寸：长边限制在 MAX_LONG_SIDE 内，缩放系数在 [MIN_SCALE, MAX_SCALE] 之间
                                val baseW = p.width
                                val baseH = p.height
                                val maxSide = maxOf(baseW, baseH)
                                val rawScale = MAX_LONG_SIDE.toFloat() / maxSide.toFloat()
                                val scale = rawScale.coerceIn(MIN_SCALE, MAX_SCALE)

                                val targetW = (baseW * scale).toInt().coerceAtLeast(1)
                                val targetH = (baseH * scale).toInt().coerceAtLeast(1)

                                val bitmap = Bitmap.createBitmap(
                                    targetW, targetH, Bitmap.Config.ARGB_8888
                                )
                                // 先填白底（PDF 可能本身透明，白底更符合阅读习惯）
                                bitmap.eraseColor(AndroidColor.WHITE)
                                p.render(
                                    bitmap,
                                    null,
                                    null,
                                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                )

                                val baos = ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                                bitmap.recycle()
                                baos.toByteArray()
                            }
                        }
                    }
            }.getOrNull()
        }

    /** 提取第一页作为封面 */
    suspend fun extractCover(
        context: Context,
        pdfPath: String,
        comicId: Long
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = renderPage(pdfPath, 0) ?: return@runCatching null
            val coverFile = File(coverDir(context), "comic_$comicId.jpg")
            coverFile.outputStream().use { it.write(bytes) }
            coverFile.absolutePath
        }.getOrNull()
    }
}