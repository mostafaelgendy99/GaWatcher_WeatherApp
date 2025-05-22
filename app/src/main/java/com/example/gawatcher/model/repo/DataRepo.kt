package com.example.gawatcher.model.repo

import com.example.gawatcher.model.pojos.WeatherFourDays
import com.example.gawatcher.model.remote.RemoteDataSource

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
    ): Result<WeatherFourDays> {
        //making a singltone object of the repository



        val remoteResult = remoteDataSource.getWeatherForecast( lat = latitude, lon = longitude, apiKey = apiKey, units = units ,language = language
        )
        return remoteResult
    }
}