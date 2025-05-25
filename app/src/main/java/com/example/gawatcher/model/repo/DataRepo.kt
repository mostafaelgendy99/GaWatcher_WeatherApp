package com.example.gawatcher.model.repo

import android.util.Log
import com.example.gawatcher.model.pojos.WeatherCurrent
import com.example.gawatcher.model.pojos.WeatherFiveDays
import com.example.gawatcher.model.remote.RemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataRepo(
    private val remoteDataSource: RemoteDataSource,
) {
    companion object {
        @Volatile
        private var instance: DataRepo? = null

        fun getInstance(remoteDataSource: RemoteDataSource): DataRepo {
            return instance ?: synchronized(this) {
                instance ?: DataRepo(remoteDataSource).also { instance = it }
            }
        }
    }

    suspend fun getWeatherForecast(
        latitude: Double,
        longitude: Double,
        apiKey: String,
        units: String = "metric",
        language: String = "en"
    ): Result<WeatherFiveDays> = withContext(Dispatchers.IO)  {
        try {
            Log.d("DataRepo", "Fetching weather: lat=$latitude, lon=$longitude, apiKey=$apiKey")
            val remoteResult = remoteDataSource.getWeatherForecast(
                lat = latitude,
                lon = longitude,
                apiKey = apiKey,
                units = units,
                language = language
            )
            remoteResult.onSuccess {
                Log.d("DataRepo", "Fetch successful: ${it.list.size} data points")
            }.onFailure { error ->
                Log.e("DataRepo", "Fetch failed: ${error.message}", error)
            }
            remoteResult
        } catch (e: Exception) {
            Log.e("DataRepo", "Unexpected error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
        apiKey: String,
        units: String = "metric",
        language: String = "en" ): Result<WeatherCurrent> = withContext(Dispatchers.IO) {
        try {
            Log.d("DataRepo", "Fetching current weather: lat=$latitude, lon=$longitude, apiKey=$apiKey")
            val remoteResult = remoteDataSource.getCurrentWeather(
                lat = latitude,
                lon = longitude,
                apiKey = apiKey,
                units = units,
                language = language
            )
            remoteResult.onSuccess {
                Log.d("DataRepo", "Current weather fetch successful")
            }.onFailure { error ->
                Log.e("DataRepo", "Current weather fetch failed: ${error.message}", error)
            }
            remoteResult
        } catch (e: Exception) {
            Log.e("DataRepo", "Unexpected error: ${e.message}", e)
            Result.failure(e)
        }
    }
}