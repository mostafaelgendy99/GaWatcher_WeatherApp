package com.example.gawatcher.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
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
        lat = arguments?.getDouble("latitude", 0.0)?: 0.0
        lon = arguments?.getDouble("longitude", 0.0)?: 0.0


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
                binding.tvUV.text = uiState.feelsLike                // Load current weather icon from uiState
                val iconName = uiState.iconUrl
                val iconId = context?.resources?.getIdentifier(
                    iconName,
                    "drawable",
                    context?.packageName
                )?.takeIf { it != 0 } ?: R.drawable.icon_01d
                Log.d("HomeFragment", "Loading icon: iconName=$iconName, iconId=$iconId")
                Glide.with(this)
                    .load(iconId)
                    .placeholder(R.drawable.icon_01d)
                    .error(R.drawable.icon_01d)
                    .into(binding.ivWeatherIcon)
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

            binding.swipeRefreshLayout.setOnRefreshListener {
            // Reset lat/lon from bundle (or 0 if not present)
                lat  = 0.0
                lon = 0.0
                fusedLocationClient =
                    LocationServices.getFusedLocationProviderClient(requireActivity())
                checkAndRequestLocationPermissions()
                // Stop refreshing in callback inside location result
                binding.swipeRefreshLayout.isRefreshing = false

            }

            if (lat != 0.0 && lon != 0.0) {
                viewModel.updateLocation(lat, lon)
            }
            else {
                fusedLocationClient =
                    LocationServices.getFusedLocationProviderClient(requireActivity())
                checkAndRequestLocationPermissions()
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
            // Permissions granted, get location
            requestLocationUpdates()
        } else {
            // Request permissions
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
            // Permissions granted, get location
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
                    // Pass coordinates to ViewModel
                    viewModel.updateLocation(latitude, longitude)
                    // Stop updates after getting the first valid location (optional)
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