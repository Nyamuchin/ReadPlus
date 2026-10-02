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
        val bytes = withContext(Dispatchers.IO) {
            ZipArchiveManager.readBytes(data.zipPath, data.entryPath)
        } ?: throw IOException("ZIP entry not found: ${data.entryPath}")

        return SourceResult(
            source = ImageSource(Buffer().apply { write(bytes) }, options.context),
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<ZipPage> {
        override fun create(data: ZipPage, options: Options, imageLoader: ImageLoader): Fetcher =
            ZipPageFetcher(data, options)
    }
}