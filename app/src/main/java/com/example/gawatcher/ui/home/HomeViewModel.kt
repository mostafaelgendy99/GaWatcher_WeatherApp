package com.example.gawatcher.ui.home

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gawatcher.gendykey._apikey
import com.example.gawatcher.model.pojos.WeatherFourDays
import com.example.gawatcher.model.repo.DataRepo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WeatherUiState(
    val cityName: String = "Modena",
    val temperature: String = "--°C",
    val weatherCondition: String = "Unknown",
    val wind: String = "-- km/h",
    val pressure: String = "-- hPa",
    val humidity: String = "--%",
    val uvIndex: String = "--",
    val errorMessage: String? = null
)
class HomeViewModel(val dataRepo: DataRepo) : ViewModel() {
    companion object {
        const val DEFAULT_ICON_URL = "http://openweathermap.org/img/wn/01d@2x.png"
    }

    private val _uiState = MutableLiveData<WeatherUiState>()
    val uiState: LiveData<WeatherUiState> = _uiState

    private val _hourlyForecast = MutableLiveData<List<HourlyForecastItem>>()
    val hourlyForecast: LiveData<List<HourlyForecastItem>> = _hourlyForecast

    private val _dailyForecast = MutableLiveData<List<DailyForecastItem>>()
    val dailyForecast: LiveData<List<DailyForecastItem>> = _dailyForecast

    init {
        fetchWeatherFourDays()
    }

    fun fetchWeatherFourDays() {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Fetching weather for lat=44.34, lon=10.99")
                val result = dataRepo.getWeatherForecast(
                    latitude = 44.34,
                    longitude = 10.99,
                    apiKey = _apikey
                )
                result.onSuccess { weatherFourDays ->
                    processWeatherData(weatherFourDays)
                }.onFailure { error ->
                    _uiState.postValue(
                        WeatherUiState(errorMessage = error.message ?: "Network error")
                    )
                    _hourlyForecast.postValue(emptyList())
                    _dailyForecast.postValue(emptyList())
                    Log.e("HomeViewModel", "Fetch failed: ${error.message}", error)
                }
            } catch (e: Exception) {
                _uiState.postValue(
                    WeatherUiState(errorMessage = "Unexpected error: ${e.message}")
                )
                _hourlyForecast.postValue(emptyList())
                _dailyForecast.postValue(emptyList())
                Log.e("HomeViewModel", "Unexpected error: ${e.message}", e)
            }
        }
    }

    private fun processWeatherData(weatherFourDays: WeatherFourDays) {
        // Current weather
        val currentWeather = weatherFourDays.list.firstOrNull()
        if (currentWeather != null && weatherFourDays.city != null) {
            _uiState.postValue(
                WeatherUiState(
                    cityName = weatherFourDays.city.name ?: "Modena",
                    temperature = String.format("%.0f°C", currentWeather.main.temp),
                    weatherCondition = currentWeather.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                    wind = String.format("%.1f km/h", currentWeather.wind.speed * 3.6),
                    pressure = "${currentWeather.main.pressure} hPa",
                    humidity = "${currentWeather.main.humidity}%",
                    uvIndex = "N/A",
                    errorMessage = null
                )
            )
            Log.d("HomeViewModel", "Current: temp=${currentWeather.main.temp}, condition=${currentWeather.weather.firstOrNull()?.description}")
        } else {
            _uiState.postValue(
                WeatherUiState(errorMessage = "No current weather data or city info")
            )
            Log.e("HomeViewModel", "No current weather data or city info")
        }

        // Hourly forecast (next 8 hours)
        val currentTime = System.currentTimeMillis() / 1000 // Current time in seconds
        val hourlyItems = weatherFourDays.list
            .filter { data ->
                val timeUnix = data.timeUnix
                timeUnix >= currentTime && timeUnix <= currentTime + 8 * 3600 // Within next 8 hours
            }
            .take(8) // Ensure max 8 items
            .mapNotNull { data ->
                val icon = data.weather.firstOrNull()?.icon ?: return@mapNotNull null
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                    Date(data.timeUnix * 1000)
                )
                val iconUrl = "http://openweathermap.org/img/wn/$icon@2x.png"
                HourlyForecastItem(
                    time = time,
                    temperature = String.format("%.0f°C", data.main.temp),
                    iconUrl = iconUrl
                )
            }
        _hourlyForecast.postValue(hourlyItems)
        Log.d("HomeViewModel", "Hourly forecast: ${hourlyItems.size} items")

        // Daily forecast (4 days, with min/max temp and weather state)
        val dailyItems = mutableListOf<DailyForecastItem>()
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        weatherFourDays.list
            .groupBy { data ->
                calendar.timeInMillis = data.timeUnix * 1000
                calendar.get(Calendar.DAY_OF_YEAR)
            }
            .toSortedMap()
            .values
            .take(4)
            .forEach { dayData ->
                // Find representative data (around noon) for weather state and icon
                val noonData = dayData.minByOrNull { data ->
                    val time = Calendar.getInstance().apply {
                        timeInMillis = data.timeUnix * 1000
                    }
                    kotlin.math.abs(time.get(Calendar.HOUR_OF_DAY) - 12)
                }
                // Calculate min and max temperatures for the day
                val minTemp = dayData.minOfOrNull { it.main.temp } ?: return@forEach
                val maxTemp = dayData.maxOfOrNull { it.main.temp } ?: return@forEach
                noonData?.let { data ->
                    val icon = data.weather.firstOrNull()?.icon ?: return@let
                    val weatherState = data.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "Unknown"
                    val date = dateFormat.format(Date(data.timeUnix * 1000))
                    val iconUrl = "http://openweathermap.org/img/wn/$icon@2x.png"
                    dailyItems.add(
                        DailyForecastItem(
                            date = date,
                            weatherState = weatherState,
                            iconUrl = iconUrl,
                            minTemp = String.format("%.0f°C", minTemp),
                            maxTemp = String.format("%.0f°C", maxTemp)
                        )
                    )
                }
            }
        _dailyForecast.postValue(dailyItems)
        Log.d("HomeViewModel", "Daily forecast: ${dailyItems.size} items")
    }
}


