package com.rgbstudios.todomobile.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val taskId: String,
    val title: String,
    val description: String,
    val taskCompleted: Boolean,
    val starred: Boolean,
    val categoryIds: List<String> = emptyList()
)