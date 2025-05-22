package com.example.gawatcher.model.remote

import com.example.gawatcher.model.pojos.WeatherFourDays
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiServices {
    @GET("forecast")
    suspend fun getWeatherForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
         @Query("lang") language: String = "en"
    ): Response<WeatherFourDays>
}