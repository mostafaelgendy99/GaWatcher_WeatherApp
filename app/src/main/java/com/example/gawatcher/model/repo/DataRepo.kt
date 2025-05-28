package com.example.gawatcher.model.repo

import android.util.Log
import com.example.gawatcher.model.local.ILocalDataSource
import com.example.gawatcher.model.local.LocalDataSource
import com.example.gawatcher.model.pojos.WeatherCurrent
import com.example.gawatcher.model.pojos.WeatherEntity
import com.example.gawatcher.model.pojos.WeatherFiveDays
import com.example.gawatcher.model.remote.IRemoteDataSource
import com.example.gawatcher.model.remote.RemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataRepo(
    private val remoteDataSource: IRemoteDataSource,
    private val localDataSource: ILocalDataSource

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
            val remoteResult = remoteDataSource.getWeatherForecast(
                lat = latitude,
                lon = longitude,
                apiKey = apiKey,
                units = units,
                language = language
            )
            remoteResult.onSuccess {
            }.onFailure { error ->
            }
            remoteResult
        } catch (e: Exception) {
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
            val remoteResult = remoteDataSource.getCurrentWeather(
                lat = latitude,
                lon = longitude,
                apiKey = apiKey,
                units = units,
                language = language
            )
            remoteResult.onSuccess {
            }.onFailure { error ->
            }
            remoteResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getCachedWeatherById(id: Int): Result<Pair<WeatherCurrent?, WeatherFiveDays?>> = withContext(Dispatchers.IO) {
        try {
            val cached = localDataSource.getWeatherById(id)
            if (cached.first != null || cached.second != null) {
                Result.success(cached)
            } else {
                Result.failure(Exception("No cached weather data available"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllCachedWeather(): Result<List<Pair<WeatherCurrent?, WeatherFiveDays?>>> = withContext(Dispatchers.IO) {
        try {
            val cachedList = localDataSource.getAllWeather()
            if (cachedList.isNotEmpty()) {
                Result.success(cachedList)
            } else {
                Result.failure(Exception("No cached weather data available"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllWeatherEntities(): Result<List<WeatherEntity>> = withContext(Dispatchers.IO) {
        try {
            val cachedEntities = localDataSource.getAllWeatherEntities()
            if (cachedEntities.isNotEmpty()) {
                Result.success(cachedEntities)
            } else {
                Result.failure(Exception("No cached weather entities"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWeatherByyId(id: Int) = withContext(Dispatchers.IO) {
        try {
            localDataSource.deleteWeatherById(id)
        } catch (e: Exception) {
        }
    }

    suspend fun deleteAllWeather() = withContext(Dispatchers.IO) {
        try {
            localDataSource.deleteAllWeather()
        } catch (e: Exception) {
        }
    }
}