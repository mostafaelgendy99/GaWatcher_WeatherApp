package com.example.gawatcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gawatcher.model.remote.RemoteDataSource
import com.example.gawatcher.model.repo.DataRepo

class HomeViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(DataRepo.getInstance(RemoteDataSource())) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}