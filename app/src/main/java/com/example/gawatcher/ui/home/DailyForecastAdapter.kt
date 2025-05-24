package com.example.gawatcher.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gawatcher.R // Add this import
import com.example.gawatcher.databinding.ItemDailyForecastBinding

class DailyForecastAdapter :
    ListAdapter<DailyForecastItem, DailyForecastAdapter.ViewHolder>(DailyForecastDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDailyForecastBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemDailyForecastBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DailyForecastItem) {
            binding.tvDate.text = item.date
            binding.tvWeatherState.text = item.weatherState
            binding.tvMinTemp.text = item.minTemp
            binding.tvMaxTemp.text = item.maxTemp
            val iconUrl = item.iconUrl.takeIf { it.isNotBlank() } ?: HomeViewModel.DEFAULT_ICON_URL
            Glide.with(binding.ivWeatherIcon.context)
                .load(iconUrl)
                .placeholder(R.drawable.icon_01d)
                .error(R.drawable.icon_01d)
                .into(binding.ivWeatherIcon)
        }
    }

    class DailyForecastDiffCallback : DiffUtil.ItemCallback<DailyForecastItem>() {
        override fun areItemsTheSame(oldItem: DailyForecastItem, newItem: DailyForecastItem): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: DailyForecastItem, newItem: DailyForecastItem): Boolean {
            return oldItem == newItem
        }
    }
}