package com.example.posturometricapp.data.dp.converter

import androidx.room.TypeConverter

/**
 * Конвертер для сохранения списка Long в базу данных.
 * Список преобразуется в строку с разделителем запятая.
 */

class ListLongConverter {
    @TypeConverter
    fun toListLong(list: List<Long>): String {
        return list.joinToString(separator = ",")
    }

    @TypeConverter
    fun toListLong(data: String): List<Long> {
        if (data.isEmpty()) return emptyList()
        return data.split(",").map { it.toLong() }
    }
}