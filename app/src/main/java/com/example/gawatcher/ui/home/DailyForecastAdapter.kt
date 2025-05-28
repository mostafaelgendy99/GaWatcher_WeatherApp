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

    inner class ViewHolder(val binding: ItemDailyForecastBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDailyForecastBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            tvDate.text = item.date
            tvWeatherState.text = item.weatherState
            tvMinTemp.text = item.minTemp
            tvMaxTemp.text = item.maxTemp
            val iconUrl = item.iconUrl
            val iconId = root.context.resources.getIdentifier(
                iconUrl,
                "drawable",
                root.context.packageName
            )
            Glide.with(ivWeatherIcon.context)
                .load(iconId)
                .placeholder(R.drawable.icon_01d)
                .error(R.drawable.icon_01d)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .into(ivWeatherIcon)
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