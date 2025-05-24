package com.example.gawatcher.model.repo

import android.util.Log
import com.example.gawatcher.model.pojos.WeatherFourDays
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
    ): Result<WeatherFourDays> = withContext(Dispatchers.IO)  {
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
}