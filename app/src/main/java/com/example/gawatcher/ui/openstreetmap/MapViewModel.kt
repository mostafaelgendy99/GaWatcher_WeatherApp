package com.example.gawatcher.ui.openstreetmap

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gawatcher.gendykey._apikey
import com.example.gawatcher.model.pojos.WeatherEntity
import com.example.gawatcher.model.repo.DataRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MapViewModel(val repo: DataRepo) : ViewModel() {

    private val _insertedStatus = MutableLiveData<Boolean>()
    val insertedStatus : LiveData<Boolean> = _insertedStatus


    fun insertLocation(lat: Double,long : Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = getLocation(lat,long)
            if(entity == null){
                _insertedStatus.postValue(false)
            }
            else{
                repo.saveWeather(entity.currentWeather, entity.forecastWeather)
                _insertedStatus.postValue(true)
            }
        }
    }

    private suspend fun getLocation(lat : Double, lon: Double): WeatherEntity? {
        val weather5Days =  repo.getWeatherForecast(
            lat,
            lon,
            _apikey
        )

        val currentWeather = repo.getCurrentWeather(
            lat,
            lon,
            _apikey
        )

        if(weather5Days.isSuccess && currentWeather.isSuccess){
            return  WeatherEntity(
                currentWeather = currentWeather.getOrNull(),
                forecastWeather = weather5Days.getOrNull(),
            )
        }
        return null
    }

}