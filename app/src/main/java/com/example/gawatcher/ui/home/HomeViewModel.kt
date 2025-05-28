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
    val cityName: String = "Fetching location...",
    val temperature: String = "--",
    val weatherCondition: String = "Unknown",
    val wind: String = "--",
    val pressure: String = "-- hPa",
    val humidity: String = "--%",
    val feelsLike: String = "--",
    val iconUrl: String = "ic_10d",
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
        _uiState.value = WeatherUiState(errorMessage = "Loading weather data...")
        _hourlyForecast.value = emptyList()
        _dailyForecast.value = emptyList()
    }

    fun updateLocation(latitude: Double, longitude: Double, tempUnit: String, windUnit: String, language: String, weatherId: Int = -1) {
        Log.d("HomeViewModel", "Received: Lat=$latitude, Lon=$longitude, TempUnit=$tempUnit, WindUnit=$windUnit, Language=$language, WeatherId=$weatherId")
        fetchWeatherData(latitude, longitude, tempUnit, windUnit, language, weatherId)
    }

    private fun fetchWeatherData(latitude: Double, longitude: Double, tempUnit: String, windUnit: String, language: String, weatherId: Int) {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Fetching weather for lat=$latitude, lon=$longitude, lang=$language")
                val langCode = if (language.equals("arabic", ignoreCase = true)) "ar" else "en"
                val currentWeatherResult = dataRepo.getCurrentWeather(latitude, longitude, _apikey, langCode)
                val forecastResult = dataRepo.getWeatherForecast(latitude, longitude, _apikey, langCode)
                if (currentWeatherResult.isSuccess && forecastResult.isSuccess) {
                    val currentWeather = currentWeatherResult.getOrNull()
                    val weatherFiveDays = forecastResult.getOrNull()
                    if (currentWeather != null && weatherFiveDays != null) {
                        processWeatherData(currentWeather, weatherFiveDays, tempUnit, windUnit, language)
                        return@launch
                    }
                }
                Log.e("HomeViewModel", "Retrofit failed, checking Room for weatherId=$weatherId")
                if (weatherId != -1) {
                    val cachedResult = dataRepo.getCachedWeatherById(weatherId)
                    if (cachedResult.isSuccess) {
                        val (cachedCurrent, cachedForecast) = cachedResult.getOrNull() ?: (null to null)
                        if (cachedCurrent != null && cachedForecast != null) {
                            processWeatherData(cachedCurrent, cachedForecast, tempUnit, windUnit, language)
                            Log.d("HomeViewModel", "Loaded cached data for weatherId=$weatherId")
                            return@launch
                        }
                    }
                }
                _uiState.postValue(WeatherUiState(
                    cityName = "No data available",
                    errorMessage = "Unable to fetch weather data. Please check your connection or try again."
                ))
                _hourlyForecast.postValue(emptyList())
                _dailyForecast.postValue(emptyList())
                Log.e("HomeViewModel", "No data available: Retrofit failed and no cached data for weatherId=$weatherId")
            } catch (e: Exception) {
                _uiState.postValue(WeatherUiState(
                    cityName = "No data available",
                    errorMessage = "Unexpected error: ${e.message}"
                ))
                _hourlyForecast.postValue(emptyList())
                _dailyForecast.postValue(emptyList())
                Log.e("HomeViewModel", "Unexpected error: ${e.message}", e)
            }
        }
    }

    private fun processWeatherData(currentWeather: WeatherCurrent, weatherFiveDays: WeatherFiveDays, tempUnit: String, windUnit: String, language: String) {
        val timezoneOffset = currentWeather.timezone ?: 0
        val currentTime = (System.currentTimeMillis() / 1000) + timezoneOffset
        val calendar = Calendar.getInstance().apply { timeInMillis = currentTime * 1000 }
        val isDayTime = calendar.get(Calendar.HOUR_OF_DAY) in 6..17
        var icon = currentWeather.weather.firstOrNull()?.icon ?: "01d"
        icon = if (isDayTime && icon.endsWith("n")) icon.dropLast(1) + "d"
        else if (!isDayTime && icon.endsWith("d")) icon.dropLast(1) + "n"
        else icon
        Log.d("HomeViewModel", "Current: icon=$icon, isDayTime=$isDayTime")

        val isCelsius = tempUnit.equals("celsius", ignoreCase = true)
        val isKmh = windUnit.equals("km/h", ignoreCase = true)
        val locale = if (language.equals("arabic", ignoreCase = true)) Locale("ar") else Locale("en")

        val temp = if (isCelsius) currentWeather.main.temp else (currentWeather.main.temp * 9/5) + 32
        val feelsLike = if (isCelsius) currentWeather.main.feelsLike else (currentWeather.main.feelsLike * 9/5) + 32
        val tempUnitSymbol = if (isCelsius) "°C" else "°F"
        val windSpeed = if (isKmh) currentWeather.wind.speed * 3.6 else currentWeather.wind.speed * 2.237
        val windUnitSymbol = if (isKmh) "km/h" else "mph"

        _uiState.postValue(
            WeatherUiState(
                cityName = currentWeather.name ?: "Current location",
                temperature = String.format(locale, "%.0f%s", temp, tempUnitSymbol),
                weatherCondition = currentWeather.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercaseChar() } ?: "Unknown",
                wind = String.format(locale, "%.1f %s", windSpeed, windUnitSymbol),
                pressure = String.format(locale, "%d hPa", currentWeather.main.pressure),
                humidity = String.format(locale, "%d%%", currentWeather.main.humidity),
                feelsLike = String.format(locale, "%.0f%s", feelsLike, tempUnitSymbol),
                iconUrl = "ic_$icon",
                errorMessage = null
            )
        )

        val timeFormat = SimpleDateFormat("h:mm a", locale)
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
                    temperature = String.format(locale, "%.0f%s", hourlyTemp, tempUnitSymbol),
                    iconUrl = "ic_$hourlyIcon"
                )
            }
        _hourlyForecast.postValue(hourlyItems)

        val dateFormat = SimpleDateFormat("EEE, MMM d", locale)
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
                            weatherState = data.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercaseChar() } ?: "Unknown",
                            iconUrl = "ic_$dailyIcon",
                            minTemp = String.format(locale, "%.0f%s", minTempConverted, tempUnitSymbol),
                            maxTemp = String.format(locale, "%.0f%s", maxTempConverted, tempUnitSymbol)
                        )
                    )
                }
            }
        _dailyForecast.postValue(dailyItems)
    }
}



