package com.readplus

import android.app.Application
import android.graphics.Bitmap
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.readplus.data.source.ZipPageFetcher
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class ReadPlusApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        // 优化 OKHttp：加大连接池、禁用重试、缩短超时（本地文件加载用不上网络）
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()

        return ImageLoader.Builder(this)
            .components { add(ZipPageFetcher.Factory()) }
            .callFactory(okHttpClient)
            // 关闭渐显：图片立即显示，视觉"更跟手"
            .crossfade(false)
            // 允许硬件位图，减少内存拷贝
            .allowHardware(true)
            // 使用 RGB_565：内存减半，手机屏幕几乎看不出画质差异（漫画封面/视频缩略图够用）
            .bitmapConfig(Bitmap.Config.RGB_565)
            // 内存缓存加大到 25%
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            // 磁盘缓存 256MB
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            // 内存+磁盘缓存策略：有则用，不则读不则写
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}