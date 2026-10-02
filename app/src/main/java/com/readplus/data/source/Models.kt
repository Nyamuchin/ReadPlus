package com.readplus.data.source

data class ZipImageEntry(
    val name: String,       // 平铺后的文件名（不含目录）
    val entryPath: String   // ZIP 内的完整路径
)

data class ZipPage(
    val zipPath: String,
    val entryPath: String
)