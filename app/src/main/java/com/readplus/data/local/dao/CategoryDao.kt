package com.readplus.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.readplus.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder ASC, createdAt ASC")
    fun observeByType(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder ASC")
    suspend fun listByType(type: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun findById(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories WHERE name = :name AND type = :type")
    suspend fun countByNameAndType(name: String, type: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Update suspend fun update(category: CategoryEntity)

    @Delete suspend fun delete(category: CategoryEntity)

    // ---- 备份用 ----
    @Query("SELECT * FROM categories")
    suspend fun getAllOnce(): List<CategoryEntity>
}