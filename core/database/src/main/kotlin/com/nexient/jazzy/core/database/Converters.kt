package com.nexient.jazzy.core.database

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun intListToString(value: List<Int>): String = value.joinToString(",")

    @TypeConverter
    fun stringToIntList(value: String): List<Int> =
        if (value.isBlank()) emptyList() else value.split(',').mapNotNull { it.trim().toIntOrNull() }
}
