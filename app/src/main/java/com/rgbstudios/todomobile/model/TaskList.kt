package com.rgbstudios.todomobile.model

import com.rgbstudios.todomobile.data.entity.TaskEntity

data class TaskList(val name: String, val list: List<TaskEntity>)