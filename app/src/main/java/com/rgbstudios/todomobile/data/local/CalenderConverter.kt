package com.rgbstudios.todomobile.data.local

import androidx.room.TypeConverter
import java.util.Calendar

object CalenderConverter {
    @TypeConverter
    fun calendarToLong(calendar: Calendar?): Long? {
        return calendar?.timeInMillis
    }

    @TypeConverter
    fun longToCalendar(value: Long?): Calendar? {
        return value?.let {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = it
            calendar
        }
    }
}