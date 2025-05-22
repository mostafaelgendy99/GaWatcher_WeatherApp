package com.example.gawatcher.model.remote

import com.example.gawatcher.model.pojos.WeatherFourDays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intellij.lang.annotations.Language


class RemoteDataSource {
    private val apiService: ApiServices = RetrofitClient.weatherApiService

    suspend fun getWeatherForecast(lat: Double, lon: Double, apiKey: String , units : String , language: String): Result<WeatherFourDays> {
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
}