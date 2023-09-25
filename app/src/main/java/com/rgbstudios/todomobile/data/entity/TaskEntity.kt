package com.rgbstudios.todomobile.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val taskId: String,
    val title: String,
    val description: String,
    val taskCompleted: Boolean,
    val starred: Boolean,
    val dueDateTime: Calendar?,
    val categoryIds: List<String> = emptyList()
)