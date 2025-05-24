package com.example.gawatcher.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.gawatcher.R
import com.example.gawatcher.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var hourlyAdapter: HourlyForecastAdapter
    private lateinit var dailyAdapter: DailyForecastAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        try {
            viewModel = ViewModelProvider(this, HomeViewModelFactory()).get(HomeViewModel::class.java)
            hourlyAdapter = HourlyForecastAdapter()
            dailyAdapter = DailyForecastAdapter()

            binding.rvHourlyForecast.addItemDecoration(
                DividerItemDecoration(context, LinearLayoutManager.HORIZONTAL)
            )
            binding.rvDailyForecast.addItemDecoration(
                DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
            )

            // Setup RecyclerViews
            binding.rvHourlyForecast.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = hourlyAdapter
            }
            binding.rvDailyForecast.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = dailyAdapter
            }
            // Observe UI state
            viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
                binding.tvCityName.text = uiState.cityName
                binding.tvTemperature.text = uiState.temperature
                binding.tvWeatherCondition.text = uiState.weatherCondition
                binding.tvWind.text = uiState.wind
                binding.tvPressure.text = uiState.pressure
                binding.tvHumidity.text = uiState.humidity
                binding.tvUV.text = uiState.uvIndex
                if (uiState.errorMessage != null) {
                    binding.tvCityName.text = uiState.errorMessage
                    binding.tvWeatherCondition.text = "Error"
                }
                Log.d("HomeFragment", "UI updated: city=${uiState.cityName}, temp=${uiState.temperature}, error=${uiState.errorMessage}")
            }
            // Observe forecast data
            viewModel.hourlyForecast.observe(viewLifecycleOwner) { hourlyItems ->
                hourlyAdapter.submitList(hourlyItems)
                Log.d("HomeFragment", "Hourly forecast updated: ${hourlyItems.size} items")
            }
            viewModel.dailyForecast.observe(viewLifecycleOwner) { dailyItems ->
                dailyAdapter.submitList(dailyItems)
                Log.d("HomeFragment", "Daily forecast updated: ${dailyItems.size} items")
            }

            // Load current weather icon
            viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
                val firstHourlyItem = viewModel.hourlyForecast.value?.firstOrNull()

                val iconUrl = firstHourlyItem?.iconUrl

                Glide.with(binding.root.context)
                    .load(iconUrl)
                    .placeholder(R.drawable.icon_01d) // Use local drawable
                    .error(R.drawable.icon_01d) // Use local drawable
                    .into(binding.ivWeatherIcon) //
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error in onCreateView: ${e.message}", e)
            binding.tvCityName.text = "Error"
            binding.tvWeatherCondition.text = "Initialization failed"
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}