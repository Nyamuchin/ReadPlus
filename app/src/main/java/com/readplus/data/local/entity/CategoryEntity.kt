package com.readplus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,          // "COMIC" or "VIDEO"
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)