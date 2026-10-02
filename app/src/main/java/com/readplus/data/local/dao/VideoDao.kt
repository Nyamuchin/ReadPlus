package com.readplus.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.readplus.data.local.entity.VideoCategoryCrossRef
import com.readplus.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

data class VideoCategoryNameRow(val videoId: Long, val categoryName: String)

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun findById(id: Long): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: VideoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<VideoEntity>): List<Long>

    @Update suspend fun update(video: VideoEntity)

    @Delete suspend fun delete(video: VideoEntity)

    @Query("""
        SELECT v.* FROM videos v
        INNER JOIN video_category_cross_ref r ON v.id = r.videoId
        WHERE r.categoryId = :categoryId
        ORDER BY v.createdAt DESC
    """)
    fun observeByCategory(categoryId: Long): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addToCategory(ref: VideoCategoryCrossRef)

    @Query("DELETE FROM video_category_cross_ref WHERE videoId = :videoId AND categoryId = :categoryId")
    suspend fun removeFromCategory(videoId: Long, categoryId: Long)

    @Query("""
        SELECT vc.videoId AS videoId, c.name AS categoryName
        FROM video_category_cross_ref vc
        INNER JOIN categories c ON vc.categoryId = c.id
    """)
    suspend fun getAllVideoCategoryNames(): List<VideoCategoryNameRow>

    // ---- 备份用 ----
    @Query("SELECT * FROM videos")
    suspend fun getAllOnce(): List<VideoEntity>

    @Query("SELECT * FROM video_category_cross_ref")
    suspend fun getAllRefs(): List<VideoCategoryCrossRef>
}