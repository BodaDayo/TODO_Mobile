package com.rgbstudios.todomobile.data.local

import androidx.room.TypeConverter

class ListStringConverter {

    @TypeConverter
    fun fromListString(value: String?): List<String> {
        return value?.split(",")?.map { it.trim() } ?: emptyList()
    }

    @TypeConverter
    fun toListString(value: List<String>): String {
        return value.joinToString(",")
    }
}
