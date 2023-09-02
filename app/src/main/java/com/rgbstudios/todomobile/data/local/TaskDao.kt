package com.rgbstudios.todomobile.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rgbstudios.todomobile.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Database access object to access the TodoApp database
 */
@Dao
interface TaskDao  {

    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<TaskEntity>>

    // Specify the conflict strategy as REPLACE, when the user tries to add an
    // existing Item into the database.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(taskEntity: TaskEntity): Long

    @Update
    suspend fun updateTask(taskEntity: TaskEntity): Int

    @Query("DELETE FROM tasks WHERE taskId = :taskId")
    suspend fun deleteTask(taskId: String): Int

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTasks(tasks: List<TaskEntity>): List<Long>
}