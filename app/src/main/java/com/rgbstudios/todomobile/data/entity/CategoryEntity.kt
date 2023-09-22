package com.rgbstudios.todomobile.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val categoryId: String,
    val categoryName: String,
    val categoryIconIdentifier: String,
    val categoryColorIdentifier: String,
)
