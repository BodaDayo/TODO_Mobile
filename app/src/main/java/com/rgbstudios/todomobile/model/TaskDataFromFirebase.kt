package com.rgbstudios.todomobile.model

data class TaskDataFromFirebase(val taskId: String, val title: String, val description: String, val taskCompleted: Boolean)