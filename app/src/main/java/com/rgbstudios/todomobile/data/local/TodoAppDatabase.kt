package com.rgbstudios.todomobile.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rgbstudios.todomobile.data.entity.CategoryEntity
import com.rgbstudios.todomobile.data.entity.TaskEntity
import com.rgbstudios.todomobile.data.entity.UserEntity

@Database(entities = [TaskEntity::class, UserEntity::class, CategoryEntity::class], version = 1, exportSchema = false)
@TypeConverters(ListStringConverter::class)
abstract class TodoAppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: TodoAppDatabase? = null

        fun getDatabase(context: Context): TodoAppDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TodoAppDatabase::class.java,
                    "user_database"
                )
                    // Wipes and rebuilds instead of migrating if no Migration object.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}
