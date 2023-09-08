package com.rgbstudios.todomobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rgbstudios.todomobile.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Database access object to access the TodoApp database
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getUser(): Flow<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(userEntity: UserEntity): Int

    @Query("DELETE FROM users")
    suspend fun deleteUser()

}