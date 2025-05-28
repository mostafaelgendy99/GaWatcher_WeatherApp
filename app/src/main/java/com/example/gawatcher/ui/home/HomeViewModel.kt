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
            errorMessage = "Loading weather "
        )
        _hourlyForecast.value = emptyList()
        _dailyForecast.value = emptyList()
    }

    fun updateLocation(latitude: Double, longitude: Double, tempUnit: String, windUnit: String) {
        Log.d("HomeViewModel", "Received location: Lat=$latitude, Lon=$longitude, TempUnit=$tempUnit, WindUnit=$windUnit")
        fetchWeatherData(latitude, longitude, tempUnit, windUnit)
    }

    private fun fetchWeatherData(latitude: Double, longitude: Double, tempUnit: String, windUnit: String) {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Fetching weather for lat=$latitude, lon=$longitude")
                val currentWeatherResult = dataRepo.getCurrentWeather(latitude, longitude, _apikey)
                val forecastResult = dataRepo.getWeatherForecast(latitude, longitude, _apikey)
                if (currentWeatherResult.isSuccess && forecastResult.isSuccess) {
                    val currentWeather = currentWeatherResult.getOrNull()
                    val weatherFiveDays = forecastResult.getOrNull()
                    if (currentWeather != null && weatherFiveDays != null) {
                        processWeatherData(currentWeather, weatherFiveDays, tempUnit, windUnit)
                    } else {
                        _uiState.postValue(WeatherUiState(errorMessage = "No weather data available"))
                        _hourlyForecast.postValue(emptyList())
                        _dailyForecast.postValue(emptyList())
                        Log.e("HomeViewModel", "No current or forecast data")
                    }
                } else {
                    val errorMessage = currentWeatherResult.exceptionOrNull()?.message
                        ?: forecastResult.exceptionOrNull()?.message
                        ?: "Network error"
                    _uiState.postValue(WeatherUiState(errorMessage = errorMessage))
                    _hourlyForecast.postValue(emptyList())
                    _dailyForecast.postValue(emptyList())
                    Log.e("HomeViewModel", "Fetch failed: $errorMessage")
                }
            } catch (e: Exception) {
                _uiState.postValue(WeatherUiState(errorMessage = "Unexpected error: ${e.message}"))
                _hourlyForecast.postValue(emptyList())
                _dailyForecast.postValue(emptyList())
                Log.e("HomeViewModel", "Unexpected error: ${e.message}", e)
            }
        }
    }

    private fun processWeatherData(currentWeather: WeatherCurrent, weatherFiveDays: WeatherFiveDays, tempUnit: String, windUnit: String) {
        // Timezone and icon adjustment
        val timezoneOffset = currentWeather.timezone ?: 0
        val currentTime = (System.currentTimeMillis() / 1000) + timezoneOffset
        val calendar = Calendar.getInstance().apply { timeInMillis = currentTime * 1000 }
        val isDayTime = calendar.get(Calendar.HOUR_OF_DAY) in 6..17
        var icon = currentWeather.weather.firstOrNull()?.icon ?: "01d"
        icon = if (isDayTime && icon.endsWith("n")) icon.dropLast(1) + "d"
        else if (!isDayTime && icon.endsWith("d")) icon.dropLast(1) + "n"
        else icon
        Log.d("HomeViewModel", "Current: icon=$icon, isDayTime=$isDayTime")

        // Temperature and wind speed conversion
        val isCelsius = tempUnit.equals("celsius", ignoreCase = true)
        val isKmh = windUnit.equals("km/h", ignoreCase = true)

        val temp = if (isCelsius) currentWeather.main.temp else (currentWeather.main.temp * 9/5) + 32
        val feelsLike = if (isCelsius) currentWeather.main.feelsLike else (currentWeather.main.feelsLike * 9/5) + 32
        val tempUnitSymbol = if (isCelsius) "°C" else "°F"
        val windSpeed = if (isKmh) currentWeather.wind.speed * 3.6 else currentWeather.wind.speed * 2.237
        val windUnitSymbol = if (isKmh) "km/h" else "mph"

        // Current weather
        _uiState.postValue(
            WeatherUiState(
                cityName = currentWeather.name ?: "Current location",
                temperature = String.format("%.0f%s", temp, tempUnitSymbol),
                weatherCondition = currentWeather.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                wind = String.format("%.1f %s", windSpeed, windUnitSymbol),
                pressure = "${currentWeather.main.pressure} hPa",
                humidity = "${currentWeather.main.humidity}%",
                feelsLike = String.format("%.0f%s", feelsLike, tempUnitSymbol),
                iconUrl = "ic_$icon",
                errorMessage = null
            )
        )

        // Hourly forecast
        val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
        val hourlyItems = weatherFiveDays.list
            .filter { data -> data.timeUnix >= currentTime && data.timeUnix <= currentTime + 24 * 3600 }
            .take(8)
            .mapNotNull { data ->
                var hourlyIcon = data.weather.firstOrNull()?.icon ?: return@mapNotNull null
                val forecastTime = data.timeUnix + timezoneOffset
                val isForecastDayTime = Calendar.getInstance().apply { timeInMillis = forecastTime * 1000 }
                    .get(Calendar.HOUR_OF_DAY) in 6..17
                hourlyIcon = if (isForecastDayTime && hourlyIcon.endsWith("n")) hourlyIcon.dropLast(1) + "d"
                else if (!isForecastDayTime && hourlyIcon.endsWith("d")) hourlyIcon.dropLast(1) + "n"
                else hourlyIcon
                val hourlyTemp = if (isCelsius) data.main.temp else (data.main.temp * 9/5) + 32
                Log.d("HomeViewModel", "Hourly: icon=$hourlyIcon, time=${data.timeUnix}, isDayTime=$isForecastDayTime")
                HourlyForecastItem(
                    time = timeFormat.format(Date(data.timeUnix * 1000)),
                    temperature = String.format("%.0f%s", hourlyTemp, tempUnitSymbol),
                    iconUrl = "ic_$hourlyIcon"
                )
            }
        _hourlyForecast.postValue(hourlyItems)

        // Daily forecast
        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.US)
        val dailyItems = mutableListOf<DailyForecastItem>()
        weatherFiveDays.list
            .groupBy { data ->
                Calendar.getInstance().apply { timeInMillis = (data.timeUnix + timezoneOffset) * 1000 }
                    .get(Calendar.DAY_OF_YEAR)
            }
            .toSortedMap()
            .values
            .take(5)
            .forEach { dayData ->
                val noonData = dayData.minByOrNull { data ->
                    val time = Calendar.getInstance().apply { timeInMillis = (data.timeUnix + timezoneOffset) * 1000 }
                    kotlin.math.abs(time.get(Calendar.HOUR_OF_DAY) - 12)
                }
                val minTemp = dayData.minOfOrNull { it.main.temp } ?: return@forEach
                val maxTemp = dayData.maxOfOrNull { it.main.temp } ?: return@forEach
                noonData?.let { data ->
                    var dailyIcon = data.weather.firstOrNull()?.icon ?: "01d"
                    dailyIcon = if (dailyIcon.endsWith("n")) dailyIcon.dropLast(1) + "d" else dailyIcon
                    val minTempConverted = if (isCelsius) minTemp else (minTemp * 9/5) + 32
                    val maxTempConverted = if (isCelsius) maxTemp else (maxTemp * 9/5) + 32
                    Log.d("HomeViewModel", "Daily: icon=$dailyIcon, time=${data.timeUnix}")
                    dailyItems.add(
                        DailyForecastItem(
                            date = dateFormat.format(Date(data.timeUnix * 1000)),
                            weatherState = data.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                            iconUrl = "ic_$dailyIcon",
                            minTemp = String.format("%.0f%s", minTempConverted, tempUnitSymbol),
                            maxTemp = String.format("%.0f%s", maxTempConverted, tempUnitSymbol)
                        )
                    )
                }
            }
        _dailyForecast.postValue(dailyItems)
    }
}



