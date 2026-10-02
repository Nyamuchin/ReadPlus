package com.readplus.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.readplus.data.local.entity.ComicCategoryCrossRef
import com.readplus.data.local.entity.ComicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComicDao {
    @Query("SELECT * FROM comics ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ComicEntity>>

    @Query("SELECT * FROM comics WHERE id = :id")
    suspend fun findById(id: Long): ComicEntity?

    @Query("SELECT * FROM comics WHERE id = :id")
    fun observeById(id: Long): Flow<ComicEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comic: ComicEntity): Long

    @Update
    suspend fun update(comic: ComicEntity)

    @Delete
    suspend fun delete(comic: ComicEntity)

    @Query("UPDATE comics SET lastReadPage = :page, lastReadAt = :ts WHERE id = :id")
    suspend fun updateProgress(id: Long, page: Int, ts: Long = System.currentTimeMillis())

    @Query("""
        SELECT c.* FROM comics c
        INNER JOIN comic_category_cross_ref r ON c.id = r.comicId
        WHERE r.categoryId = :categoryId
        ORDER BY c.createdAt DESC
    """)
    fun observeByCategory(categoryId: Long): Flow<List<ComicEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addToCategory(ref: ComicCategoryCrossRef)

    @Query("DELETE FROM comic_category_cross_ref WHERE comicId = :comicId AND categoryId = :categoryId")
    suspend fun removeFromCategory(comicId: Long, categoryId: Long)

    // ---- 备份用 ----
    @Query("SELECT * FROM comics")
    suspend fun getAllOnce(): List<ComicEntity>

    @Query("SELECT * FROM comic_category_cross_ref")
    suspend fun getAllRefs(): List<ComicCategoryCrossRef>
}