package com.rgbstudios.todomobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rgbstudios.todomobile.TodoMobileApplication

class TodoViewModelFactory(private val application: TodoMobileApplication) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TodoViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}