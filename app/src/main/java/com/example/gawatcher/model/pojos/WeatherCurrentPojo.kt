package com.example.gawatcher.model.pojos

import com.google.gson.annotations.SerializedName

data class WeatherCurrent(
    val coord: CoordCurrent,
    val weather: List<WeatherDataCurrent>,
    val base: String,
    val main: MainCurrent,
    val visibility: Long,
    val wind: WindCurrent,
    val clouds: CloudsCurrent,
    val dt: Long,
    val sys: SysCurrent,
    val timezone: Long,
    val id: Long,
    val name: String,
    val cod: Long
)

data class CoordCurrent(
    val lon: Double,
    val lat: Double
)

data class WeatherDataCurrent(
    val id: Long,
    val main: String,
    val description: String,
    val icon: String
)

data class MainCurrent(
    val temp: Double,
    @SerializedName("feels_like")
    val feelsLike: Double,
    @SerializedName("temp_min")
    val tempMin: Double,
    @SerializedName("temp_max")
    val tempMax: Double,
    val pressure: Long,
    val humidity: Long,
    @SerializedName("sea_level")
    val seaLevel: Long,
    @SerializedName("grnd_level")
    val grndLevel: Long
)

data class WindCurrent(
    val speed: Double,
    val deg: Long,
    val gust: Double
)

data class CloudsCurrent(
    val all: Long
)

data class SysCurrent(
    val type: Long,
    val id: Long,
    val country: String,
    val sunrise: Long,
    val sunset: Long
)