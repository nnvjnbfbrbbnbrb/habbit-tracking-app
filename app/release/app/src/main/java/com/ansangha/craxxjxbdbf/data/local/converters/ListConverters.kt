package com.ansangha.craxxjxbdbf.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ListConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<Long>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<Long> {
        if (value.isBlank()) return emptyList()
        val type = object : TypeToken<List<Long>>() {}.type
        return try {
            gson.fromJson(value, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}