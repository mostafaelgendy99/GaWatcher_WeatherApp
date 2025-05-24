package com.example.gawatcher.ui.home

data class HourlyForecastItem(
    val time: String,
    val temperature: String,
    val iconUrl: String
)

data class DailyForecastItem(
    val date: String,
    val weatherState: String,
    val iconUrl: String,
    val minTemp: String,
    val maxTemp: String
)