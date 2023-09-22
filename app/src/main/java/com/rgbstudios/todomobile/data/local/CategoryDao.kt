package com.rgbstudios.todomobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rgbstudios.todomobile.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity): Int

    @Query("DELETE FROM categories WHERE categoryId = :categoryId")
    suspend fun deleteCategory(categoryId: String): Int

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCategories(categories: List<CategoryEntity>): List<Long>
}
