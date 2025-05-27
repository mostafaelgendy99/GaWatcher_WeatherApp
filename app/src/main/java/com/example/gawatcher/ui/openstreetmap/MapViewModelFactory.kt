package com.example.gawatcher.ui.openstreetmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gawatcher.model.remote.RemoteDataSource
import com.example.gawatcher.model.repo.DataRepo
import com.example.gawatcher.ui.home.HomeViewModel

class MapViewModelFactory (private val repo: DataRepo): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}