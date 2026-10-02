package com.readplus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.readplus.data.local.dao.CategoryDao
import com.readplus.data.local.dao.ComicDao
import com.readplus.data.local.dao.VideoDao
import com.readplus.data.local.entity.*

@Database(
    entities = [
        ComicEntity::class,
        VideoEntity::class,
        CategoryEntity::class,
        ComicCategoryCrossRef::class,
        VideoCategoryCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ReadPlusDatabase : RoomDatabase() {
    abstract fun comicDao(): ComicDao
    abstract fun videoDao(): VideoDao
    abstract fun categoryDao(): CategoryDao
}