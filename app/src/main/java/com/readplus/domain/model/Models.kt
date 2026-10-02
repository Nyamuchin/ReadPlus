package com.readplus.domain.model

enum class ReadingMode { LEFT_TO_RIGHT, RIGHT_TO_LEFT, VERTICAL_SCROLL }

data class Comic(
    val id: Long,
    val title: String,
    val zipPath: String,
    val pageCount: Int,
    val coverPath: String?,
    val lastReadPage: Int,
    val lastReadAt: Long
)

data class Video(
    val id: Long,
    val title: String,
    val uri: String,
    val duration: Long,
    val coverPath: String?
)

data class Category(
    val id: Long,
    val name: String,
    val type: String
)