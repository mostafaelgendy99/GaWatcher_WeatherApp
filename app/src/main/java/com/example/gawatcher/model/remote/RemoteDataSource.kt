package com.example.gawatcher.model.remote

import com.example.gawatcher.model.pojos.WeatherCurrent
import com.example.gawatcher.model.pojos.WeatherFiveDays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class RemoteDataSource {
    private val apiService: ApiServices = RetrofitClient.weatherApiService

    suspend fun getWeatherForecast(lat: Double, lon: Double, apiKey: String , units : String , language: String): Result<WeatherFiveDays> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getWeatherForecast(lat, lon, apiKey)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to fetch weather data: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getCurrentWeather(lat: Double, lon: Double, apiKey: String, units: String, language: String): Result<WeatherCurrent> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCurrentWeather(lat, lon, apiKey)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to fetch current weather data: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

}