package com.example.gawatcher.ui.favorites

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gawatcher.R
import com.example.gawatcher.databinding.ItemFavoriteBinding
import com.example.gawatcher.model.pojos.WeatherCurrent
import com.example.gawatcher.model.pojos.WeatherEntity
import com.example.gawatcher.model.pojos.WeatherFiveDays

class FavoritesAdapter(
    private val onDeleteClick: (Int) -> Unit,
    private val onItemClick: (WeatherEntity) -> Unit
) : ListAdapter<WeatherEntity, FavoritesAdapter.ViewHolder>(FavoritesDiffCallback()) {

    inner class ViewHolder(val binding: ItemFavoriteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFavoriteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            tvLocationName.text = item.currentWeather?.name ?: "Unknown Location"
            btnDelete.setOnClickListener { onDeleteClick(item.id) }
            root.setOnClickListener { onItemClick(item) }
        }
    }

    class FavoritesDiffCallback : DiffUtil.ItemCallback<WeatherEntity>() {
        override fun areItemsTheSame(oldItem: WeatherEntity, newItem: WeatherEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WeatherEntity, newItem: WeatherEntity): Boolean {
            return oldItem == newItem
        }
    }
}