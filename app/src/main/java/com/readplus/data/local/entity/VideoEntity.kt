package com.readplus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val uri: String,
    val duration: Long = 0,
    val size: Long = 0,
    val coverPath: String?,
    val folderUri: String?,
    val importSource: String,          // "MANUAL" or "FOLDER"
    val createdAt: Long = System.currentTimeMillis()
)