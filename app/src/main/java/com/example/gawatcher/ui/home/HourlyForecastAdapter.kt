package com.example.gawatcher.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gawatcher.R // Add this import
import com.example.gawatcher.databinding.ItemHourlyForecastBinding

class HourlyForecastAdapter :
    ListAdapter<HourlyForecastItem, HourlyForecastAdapter.ViewHolder>(HourlyForecastDiffCallback()) {

    inner class ViewHolder(val binding: ItemHourlyForecastBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHourlyForecastBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            tvTime.text = item.time
            tvTemperature.text = item.temperature
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
                .into(ivWeatherIcon)
        }
    }

    class HourlyForecastDiffCallback : DiffUtil.ItemCallback<HourlyForecastItem>() {
        override fun areItemsTheSame(oldItem: HourlyForecastItem, newItem: HourlyForecastItem): Boolean {
            return oldItem.time == newItem.time
        }

        override fun areContentsTheSame(oldItem: HourlyForecastItem, newItem: HourlyForecastItem): Boolean {
            return oldItem == newItem
        }
    }
}