package com.example.gawatcher.model.pojos

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.gawatcher.model.local.WeatherTypeConverters

@Entity(tableName = "weather")
data class WeatherEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @TypeConverters(WeatherTypeConverters::class)
    val currentWeather: WeatherCurrent?,
    @TypeConverters(WeatherTypeConverters::class)
    val forecastWeather: WeatherFiveDays?,
    val timestamp: Long
)