package com.example.gawatcher.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.gawatcher.MainActivity
import com.example.gawatcher.R
import com.example.gawatcher.databinding.FragmentHomeBinding
import com.example.gawatcher.model.local.LocalDataSource
import com.example.gawatcher.model.local.WeatherDatabase
import com.example.gawatcher.model.remote.RemoteDataSource
import com.example.gawatcher.model.repo.DataRepo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var hourlyAdapter: HourlyForecastAdapter
    private lateinit var dailyAdapter: DailyForecastAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient //location client for getting user's location
    private val LOCATION_PERMISSION_CODE = 100 //location permission request code
    private var lat = arguments?.getDouble("latitude", 0.0)?: 0.0
    private var lon = arguments?.getDouble("longitude", 0.0)?: 0.0
    private var weatherId = -1 // Default to invalid ID
    private lateinit var senderId: String

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(
            DataRepo.getInstance(
                RemoteDataSource(),
                LocalDataSource(WeatherDatabase.getDatabase(requireContext()).weatherDao())
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        senderId = arguments?.getString("sender_id") ?: "default"
        lat = arguments?.getDouble("latitude", 0.0) ?: 0.0
        lon = arguments?.getDouble("longitude", 0.0) ?: 0.0
        weatherId = arguments?.getInt("weather_id", -1) ?: -1 // Get Room ID

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        try {
            hourlyAdapter = HourlyForecastAdapter()
            dailyAdapter = DailyForecastAdapter()

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
                binding.tvUV.text = uiState.feelsLike
                val iconName = uiState.iconUrl
                val iconId = context?.resources?.getIdentifier(
                    iconName,
                    "drawable",
                    context?.packageName
                )?.takeIf { it != 0 } ?: R.drawable.ic_01d
                Log.d("HomeFragmentIcon", "Loading icon: iconName=$iconName, iconId=$iconId")
                Glide.with(this)
                    .load(iconId)
                    .placeholder(R.drawable.ic_01d)
                    .error(R.drawable.ic_01d)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                    .into(binding.ivWeatherIcon)
                if (uiState.errorMessage != null) {
                    binding.tvCityName.text = uiState.cityName
                    binding.tvWeatherCondition.text = uiState.errorMessage
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

            binding.swipeRefreshLayout.setOnRefreshListener {
                lat = 0.0
                lon = 0.0
                weatherId = -1 // Reset ID on refresh
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
                checkAndRequestLocationPermissions()
                (activity as? MainActivity)?.updateToolbarTitle("Home")
                binding.swipeRefreshLayout.isRefreshing = false
            }
            // Load preferences and update location
            val sharedPrefs = requireActivity().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
            val tempUnit = sharedPrefs.getString("temp_unit", "celsius") ?: "celsius"
            val windUnit = sharedPrefs.getString("wind_unit", "km/h") ?: "km/h"

            if (lat != 0.0 && lon != 0.0) {
                viewModel.updateLocation(lat, lon, tempUnit, windUnit, weatherId)
                if (senderId == "MapFragment") {
                    (activity as? MainActivity)?.updateToolbarTitle("Map")
                    Log.e("sender", "MapFragment sender detected, updating toolbar title to Map")
                } else {
                    (activity as? MainActivity)?.updateToolbarTitle("Favorites")
                    Log.e("sender", "FavoritesFragment sender detected, updating toolbar title to Favorites")
                }
            } else {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
                checkAndRequestLocationPermissions()
                (activity as? MainActivity)?.updateToolbarTitle("Home")
            }

        } catch (e: Exception) {
            Log.e("HomeFragment", "Error in onCreateView: ${e.message}", e)
            binding.tvCityName.text = "Error"
            binding.tvWeatherCondition.text = "Initialization failed"
        }

        return binding.root
    }

    private fun checkAndRequestLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationUpdates()
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
            requestLocationUpdates()
        } else {
            Log.e("HomeFragment", "Location permissions denied")
            binding.tvCityName.text = "Location permissions denied"
            binding.tvWeatherCondition.text = "Cannot fetch weather"
        }
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(0).apply {
            setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val latitude = location.latitude
                    val longitude = location.longitude
                    Log.d("HomeFragment", "Location received: Lat=$latitude, Long=$longitude")
                    val sharedPrefs = requireActivity().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
                    val tempUnit = sharedPrefs.getString("temp_unit", "celsius") ?: "celsius"
                    val windUnit = sharedPrefs.getString("wind_unit", "km/h") ?: "km/h"
                    viewModel.updateLocation(latitude, longitude, tempUnit, windUnit, -1) // No ID for GPS
                    fusedLocationClient.removeLocationUpdates(this)
                } ?: run {
                    Log.e("HomeFragment", "Location unavailable")
                    binding.tvCityName.text = "Location unavailable"
                    binding.tvWeatherCondition.text = "Cannot show weather"
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}