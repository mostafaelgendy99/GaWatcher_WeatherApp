package com.example.gawatcher.model.local

import com.example.gawatcher.model.pojos.WeatherCurrent
import com.example.gawatcher.model.pojos.WeatherEntity
import com.example.gawatcher.model.pojos.WeatherFiveDays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalDataSource(private val weatherDao: WeatherDao) {

    suspend fun saveWeather(current: WeatherCurrent?, forecast: WeatherFiveDays?) {
        withContext(Dispatchers.IO) {
            if (current == null && forecast == null) return@withContext
            val weatherEntity = WeatherEntity(
                currentWeather = current,
                forecastWeather = forecast,
                timestamp = System.currentTimeMillis() / 1000 // Convert to seconds
            )
            weatherDao.insertWeather(weatherEntity)
        }
    }

    suspend fun getWeatherById(id: Int): Pair<WeatherCurrent?, WeatherFiveDays?> {
        return withContext(Dispatchers.IO) {
            val entity = weatherDao.getWeatherById(id)
            entity?.let { it.currentWeather to it.forecastWeather } ?: (null to null)
        }
    }

    suspend fun getAllWeather(): List<Pair<WeatherCurrent?, WeatherFiveDays?>> {
        return withContext(Dispatchers.IO) {
            weatherDao.getAllWeather().map { it.currentWeather to it.forecastWeather }
        }
    }

    suspend fun deleteWeatherById(id: Int) {
        withContext(Dispatchers.IO) {
            weatherDao.deleteWeatherById(id)
        }
    }

    suspend fun deleteAllWeather() {
        withContext(Dispatchers.IO) {
            weatherDao.deleteAllWeather()
        }
    }
}