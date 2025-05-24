package com.example.gawatcher.model.pojos
import com.google.gson.annotations.SerializedName

    data class WeatherFourDays(
        @SerializedName("cod") val codeNoError: String,
        @SerializedName("message") val message: Long,
        @SerializedName("cnt") val totalHoursCount: Long,
        @SerializedName("list") val list: List<WeatherData>,
        @SerializedName("city") val city: City
    )

    data class WeatherData(
        @SerializedName("dt") val timeUnix: Long,
        @SerializedName("main") val main: Main,
        @SerializedName("weather") val weather: List<Weather>,
        @SerializedName("clouds") val clouds: Clouds,
        @SerializedName("wind") val wind: Wind,
        @SerializedName("visibility") val visibility: Long?,
        @SerializedName("pop") val pop: Double,
        @SerializedName("sys") val sys: Sys,
        @SerializedName("dt_txt") val dtTxt: String,
        @SerializedName("rain") val rain: Rain?
    )

    data class Main(
        @SerializedName("temp") val temp: Double,
        @SerializedName("feels_like") val feelsLike: Double,
        @SerializedName("temp_min") val tempMin: Double,
        @SerializedName("temp_max") val tempMax: Double,
        @SerializedName("pressure") val pressure: Long,
        @SerializedName("sea_level") val seLevel: Long,
        @SerializedName("grnd_level") val grndLevel: Long,
        @SerializedName("humidity") val humidity: Long,
        @SerializedName("temp_kf") val tempKf: Double
    )

    data class Weather(
        @SerializedName("id") val id: Long,
        @SerializedName("main") val main: String,
        @SerializedName("description") val description: String,
        @SerializedName("icon") val icon: String
    )

    data class Clouds(
        @SerializedName("all") val all: Long
    )

    data class Wind(
        @SerializedName("speed") val speed: Double,
        @SerializedName("deg") val deg: Long,
        @SerializedName("gust") val gust: Double
    )

    data class Sys(
        @SerializedName("pod") val pod: String
    )

    data class Rain(
        @SerializedName("1h") val oneHour: Double
    )

    data class City(
        @SerializedName("id") val id: Long,
        @SerializedName("name") val name: String,
        @SerializedName("coord") val coord: Coord,
        @SerializedName("country") val country: String,
        @SerializedName("population") val population: Long,
        @SerializedName("timezone") val timezone: Long,
        @SerializedName("sunrise") val sunrise: Long,
        @SerializedName("sunset") val sunset: Long
    )

    data class Coord(
        @SerializedName("lat") val lat: Double,
        @SerializedName("lon") val lon: Double
    )
