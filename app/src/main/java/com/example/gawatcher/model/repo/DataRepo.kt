package com.example.gawatcher.model.repo

import android.util.Log
import com.example.gawatcher.model.local.LocalDataSource
import com.example.gawatcher.model.pojos.WeatherCurrent
import com.example.gawatcher.model.pojos.WeatherEntity
import com.example.gawatcher.model.pojos.WeatherFiveDays
import com.example.gawatcher.model.remote.RemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataRepo(
    private val remoteDataSource: RemoteDataSource,
    private val localDataSource: LocalDataSource

) {
    companion object {
        @Volatile
        private var instance: DataRepo? = null

        fun getInstance(remoteDataSource: RemoteDataSource, localDataSource: LocalDataSource): DataRepo {
            return instance ?: synchronized(this) {
                instance ?: DataRepo(remoteDataSource, localDataSource).also { instance = it }
            }
        }
    }

    suspend fun saveWeather(
        current: WeatherCurrent?,
        forecast: WeatherFiveDays?
    ) = withContext(Dispatchers.IO) {
        try {
            localDataSource.saveWeather(current, forecast)
        } catch (e: Exception) {
            Log.e("DataRepo", "Error saving weather data: ${e.message}", e)
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
    suspend fun getCachedWeatherById(id: Int): Result<Pair<WeatherCurrent?, WeatherFiveDays?>> = withContext(Dispatchers.IO) {
        try {
            Log.d("DataRepo", "Fetching cached weather for cityId=$id")
            val cached = localDataSource.getWeatherById(id)
            if (cached.first != null || cached.second != null) {
                Log.d("DataRepo", "Cached weather found for cityId=$id")
                Result.success(cached)
            } else {
                Log.w("DataRepo", "No cached weather found for cityId=$id")
                Result.failure(Exception("No cached weather data available"))
            }
        } catch (e: Exception) {
            Log.e("DataRepo", "Error fetching cached weather: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAllCachedWeather(): Result<List<Pair<WeatherCurrent?, WeatherFiveDays?>>> = withContext(Dispatchers.IO) {
        try {
            Log.d("DataRepo", "Fetching all cached weather")
            val cachedList = localDataSource.getAllWeather()
            if (cachedList.isNotEmpty()) {
                Log.d("DataRepo", "Found ${cachedList.size} cached weather entries")
                Result.success(cachedList)
            } else {
                Log.w("DataRepo", "No cached weather data available")
                Result.failure(Exception("No cached weather data available"))
            }
        } catch (e: Exception) {
            Log.e("DataRepo", "Error fetching all cached weather: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAllWeatherEntities(): Result<List<WeatherEntity>> = withContext(Dispatchers.IO) {
        try {
            Log.d("DataRepo", "Fetching all cached weather entities")
            val cachedEntities = localDataSource.getAllWeatherEntities()
            if (cachedEntities.isNotEmpty()) {
                Log.d("DataRepo", "Found ${cachedEntities.size} cached weather entities")
                Result.success(cachedEntities)
            } else {
                Log.w("DataRepo", "No cached weather entities available")
                Result.failure(Exception("No cached weather entities"))
            }
        } catch (e: Exception) {
            Log.e("DataRepo", "Error fetching cached weather entities: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteWeatherByyId(id: Int) = withContext(Dispatchers.IO) {
        try {
            Log.d("DataRepo", "Deleting cached weather for cityId=$id")
            localDataSource.deleteWeatherById(id)
            Log.d("DataRepo", "Deleted cached weather for cityId=$id")
        } catch (e: Exception) {
            Log.e("DataRepo", "Error deleting cached weather: ${e.message}", e)
        }
    }

    suspend fun deleteAllWeather() = withContext(Dispatchers.IO) {
        try {
            Log.d("DataRepo", "Deleting all cached weather")
            localDataSource.deleteAllWeather()
            Log.d("DataRepo", "Deleted all cached weather")
        } catch (e: Exception) {
            Log.e("DataRepo", "Error deleting all cached weather: ${e.message}", e)
        }
    }
}