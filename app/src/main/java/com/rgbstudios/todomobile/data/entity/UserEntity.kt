package com.rgbstudios.todomobile.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val name: String?,
    val email: String,
    val occupation: String?,
    val avatarFilePath: String?
)