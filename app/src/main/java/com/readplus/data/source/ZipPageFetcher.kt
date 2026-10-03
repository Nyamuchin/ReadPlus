package com.readplus.data.source

import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import java.io.IOException

class ZipPageFetcher(
    private val data: ZipPage,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val isPdf = data.zipPath.endsWith(".pdf", ignoreCase = true)

        val bytes: ByteArray = if (isPdf) {
            // PDF：entryPath 存的是 "0"、"1" 这样的页码
            val pageIndex = data.entryPath.toIntOrNull()
                ?: throw IOException("Invalid PDF page index: ${data.entryPath}")
            PdfArchiveManager.renderPage(data.zipPath, pageIndex)
                ?: throw IOException("Failed to render PDF page $pageIndex")
        } else {
            withContext(Dispatchers.IO) {
                ZipArchiveManager.readBytes(data.zipPath, data.entryPath)
            } ?: throw IOException("ZIP entry not found: ${data.entryPath}")
        }

        return SourceResult(
            source = ImageSource(Buffer().apply { write(bytes) }, options.context),
            // PDF 渲染返回的是 JPEG，ZIP 里的图让 Coil 自动嗅探
            mimeType = if (isPdf) "image/jpeg" else null,
            dataSource = DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<ZipPage> {
        override fun create(data: ZipPage, options: Options, imageLoader: ImageLoader): Fetcher =
            ZipPageFetcher(data, options)
    }
}