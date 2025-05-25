package com.example.gawatcher.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gawatcher.gendykey._apikey
import com.example.gawatcher.model.pojos.WeatherCurrent
import com.example.gawatcher.model.pojos.WeatherFiveDays
import com.example.gawatcher.model.repo.DataRepo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WeatherUiState(
    val cityName: String = "Cairo",
    val temperature: String = "--°C",
    val weatherCondition: String = "Unknown",
    val wind: String = "-- km/h",
    val pressure: String = "-- hPa",
    val humidity: String = "--%",
    val feelsLike: String = "--°C",
    val iconUrl: String = "ic_10d", // Added iconUrl with default
    val errorMessage: String? = null
)

class HomeViewModel(val dataRepo: DataRepo) : ViewModel() {

    private val _uiState = MutableLiveData<WeatherUiState>()
    val uiState: LiveData<WeatherUiState> = _uiState

    private val _hourlyForecast = MutableLiveData<List<HourlyForecastItem>>()
    val hourlyForecast: LiveData<List<HourlyForecastItem>> = _hourlyForecast

    private val _dailyForecast = MutableLiveData<List<DailyForecastItem>>()
    val dailyForecast: LiveData<List<DailyForecastItem>> = _dailyForecast

    init {
        // Initialize with error message until location is provided
        _uiState.value = WeatherUiState(
            errorMessage = "Location needed to show weather data. Please grant location access."
        )
        _hourlyForecast.value = emptyList()
        _dailyForecast.value = emptyList()
    }

    fun updateLocation(latitude: Double, longitude: Double) {
        Log.d("HomeViewModel", "Received location: Lat=$latitude, Long=$longitude")
        fetchWeatherData(latitude, longitude)
    }

    private fun fetchWeatherData(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Fetching weather for lat=$latitude, lon=$longitude")
                // Fetch current weather
                val currentWeatherResult = dataRepo.getCurrentWeather(
                    latitude = latitude,
                    longitude = longitude,
                    apiKey = _apikey
                )
                // Fetch forecast
                val forecastResult = dataRepo.getWeatherForecast(
                    latitude = latitude,
                    longitude = longitude,
                    apiKey = _apikey
                )
                if (currentWeatherResult.isSuccess && forecastResult.isSuccess) {
                    val currentWeather = currentWeatherResult.getOrNull()
                    val weatherFiveDays = forecastResult.getOrNull()
                    if (currentWeather != null && weatherFiveDays != null) {
                        processWeatherData(currentWeather, weatherFiveDays)
                    } else {
                        _uiState.postValue(
                            WeatherUiState(errorMessage = "No weather data available")
                        )
                        _hourlyForecast.postValue(emptyList())
                        _dailyForecast.postValue(emptyList())
                        Log.e("HomeViewModel", "No current weather or forecast data")
                    }
                } else {
                    val errorMessage = currentWeatherResult.exceptionOrNull()?.message
                        ?: forecastResult.exceptionOrNull()?.message
                        ?: "Network error"
                    _uiState.postValue(
                        WeatherUiState(errorMessage = errorMessage)
                    )
                    _hourlyForecast.postValue(emptyList())
                    _dailyForecast.postValue(emptyList())
                    Log.e("HomeViewModel", "Fetch failed: $errorMessage")
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

    private fun processWeatherData(currentWeather: WeatherCurrent, weatherFiveDays: WeatherFiveDays) {
        // Current weather
        _uiState.postValue(
            WeatherUiState(
                cityName = currentWeather.name ?: "current location",
                temperature = String.format("%.0f°C", currentWeather.main.temp),
                weatherCondition = currentWeather.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                wind = String.format("%.1f km/h", currentWeather.wind.speed * 3.6),
                pressure = "${currentWeather.main.pressure} hPa",
                humidity = "${currentWeather.main.humidity}%",
                feelsLike = String.format("%.0f°C", currentWeather.main.feelsLike), // Replaced uvIndex with feelsLike
                iconUrl = currentWeather.weather.firstOrNull()?.icon?.let { "ic_$it" } ?: "ic_01d",
                errorMessage = null
            )
        )
        Log.d("HomeViewModel", "Current: temp=${currentWeather.main.temp}, condition=${currentWeather.weather.firstOrNull()?.description}, icon=${currentWeather.weather.firstOrNull()?.icon}")

        // Hourly forecast (next 24 hours, limited to 8 items)
        val currentTime = System.currentTimeMillis() / 1000 // Current time in seconds
        val hourlyItems = weatherFiveDays.list
            .filter { data ->
                val timeUnix = data.timeUnix
                timeUnix >= currentTime && timeUnix <= currentTime + 24 * 3600 // Within next 24 hours
            }
            .take(8) // Ensure max 8 items
            .mapNotNull { data ->
                val icon = data.weather.firstOrNull()?.icon ?: return@mapNotNull null
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                    Date(data.timeUnix * 1000)
                )
                val iconUrl = "ic_$icon"
                HourlyForecastItem(
                    time = time,
                    temperature = String.format("%.0f°C", data.main.temp),
                    iconUrl = iconUrl
                )
            }
        _hourlyForecast.postValue(hourlyItems)
        Log.d("HomeViewModel", "Hourly forecast: ${hourlyItems.size} items")

        // Daily forecast (5 days, with min/max temp and weather state)
        val dailyItems = mutableListOf<DailyForecastItem>()
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        weatherFiveDays.list
            .groupBy { data ->
                calendar.timeInMillis = data.timeUnix * 1000
                calendar.get(Calendar.DAY_OF_YEAR)
            }
            .toSortedMap()
            .values
            .take(5)
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
                    val iconUrl = "ic_$icon"
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


