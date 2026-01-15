package com.hash.database

import androidx.room.TypeConverter
import com.google.gson.reflect.TypeToken
import com.hjq.gson.factory.GsonFactory
import java.lang.reflect.Type

/**
 * Room Type converters for the `core:database` module.
 * Converts List<Int> <-> JSON String using Gson.
 * Renamed to avoid name collision with other generated `Converters` classes.
 */
class RoomTypeConverters {

    private val gson = GsonFactory.getSingletonGson()

    @TypeConverter
    fun fromIntList(value: List<Int>?): String {
        // Store empty list as JSON array if null to keep DB column non-null consistent with Kotlin non-nullable fields
        return gson.toJson(value ?: emptyList<Int>())
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        if (value.isNullOrEmpty()) return emptyList()
        val listType: Type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
}
