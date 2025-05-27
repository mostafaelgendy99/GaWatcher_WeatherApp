package com.example.gawatcher.ui.favorites

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gawatcher.model.pojos.WeatherCurrent
import com.example.gawatcher.model.pojos.WeatherEntity
import com.example.gawatcher.model.pojos.WeatherFiveDays
import com.example.gawatcher.model.repo.DataRepo
import kotlinx.coroutines.launch

class FavoritesViewModel(val repo: DataRepo) : ViewModel() {

    private val _favorites = MutableLiveData<Result<List<WeatherEntity>>>()
    val favorites: LiveData<Result<List<WeatherEntity>>> = _favorites

//    private val _text = MutableLiveData<String>().apply {
//        value = "This is favorites Fragment"
//    }
//    val text: LiveData<String> = _text



    fun fetchFavorites() {
        viewModelScope.launch {
            _favorites.value = repo.getAllWeatherEntities()
        }
    }

    fun deleteFavorite(id: Int) {
        viewModelScope.launch {
            try {
                repo.deleteWeatherByyId(id)
                fetchFavorites() // Refresh the list after deletion
            } catch (e: Exception) {
                _favorites.value = Result.failure(Exception("Failed to delete: ${e.message}"))
            }
        }
    }
}