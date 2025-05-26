package com.example.gawatcher.model.local

import androidx.room.TypeConverter
import com.example.gawatcher.model.pojos.WeatherCurrent
import com.example.gawatcher.model.pojos.WeatherFiveDays
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class WeatherTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromWeatherCurrent(value: WeatherCurrent?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toWeatherCurrent(value: String?): WeatherCurrent? {
        return value?.let {
            gson.fromJson(it, object : TypeToken<WeatherCurrent>() {}.type)
        }
    }

    @TypeConverter
    fun fromWeatherFiveDays(value: WeatherFiveDays?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toWeatherFiveDays(value: String?): WeatherFiveDays? {
        return value?.let {
            gson.fromJson(it, object : TypeToken<WeatherFiveDays>() {}.type)
        }
    }
}