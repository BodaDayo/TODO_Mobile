package com.rgbstudios.todomobile

import android.app.Application
import com.rgbstudios.todomobile.data.local.TodoAppDatabase
import com.rgbstudios.todomobile.data.repository.TodoRepository

class TodoMobileApplication : Application() {

    val database: TodoAppDatabase by lazy { TodoAppDatabase.getDatabase(this) }

    val repository: TodoRepository by lazy {
        val taskDao = database.taskDao()
        val userDao = database.userDao()
        val categoryDao = database.categoryDao()
        TodoRepository(taskDao, userDao, categoryDao)
    }
}