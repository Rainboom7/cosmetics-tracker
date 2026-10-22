package io.github.rainboom7.cosmetics.data.db

import androidx.room.TypeConverter
import org.json.JSONArray
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromJsonList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        val array = JSONArray(value)
        return List(array.length()) { array.getString(it) }
    }

    @TypeConverter
    fun toJsonList(list: List<String>): String = JSONArray(list).toString()
}
