package com.example.gawatcher.model.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gawatcher.model.pojos.WeatherEntity

@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherEntity)

    @Query("SELECT * FROM weather WHERE id = :id")
    suspend fun getWeatherById(id: Int): WeatherEntity?

    @Query("SELECT * FROM weather")
    suspend fun getAllWeather(): List<WeatherEntity>

    @Query("DELETE FROM weather WHERE id = :id")
    suspend fun deleteWeatherById(id: Int)

    @Query("DELETE FROM weather")
    suspend fun deleteAllWeather()
}