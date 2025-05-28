package com.example.gawatcher.ui.settings

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.gawatcher.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        // Initialize SharedPreferences
        val sharedPrefs = requireActivity().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // Set default preferences on first launch
        if (!sharedPrefs.contains("temp_unit")) {
            editor.putString("temp_unit", "celsius").apply()
            Log.d("SettingsFragment", "Set default temp_unit to celsius")
        }
        if (!sharedPrefs.contains("wind_unit")) {
            editor.putString("wind_unit", "km/h").apply()
            Log.d("SettingsFragment", "Set default wind_unit to km/h")
        }

        // Load saved preferences
        val tempUnit = sharedPrefs.getString("temp_unit", "celsius")?.lowercase() // Default to celsius
        val windUnit = sharedPrefs.getString("wind_unit", "km/h")?.lowercase() // Default to km/h

        // Set initial radio button states
        when (tempUnit) {
            "celsius" -> binding.radioCelsius.isChecked = true
            "fahrenheit" -> binding.radioFahrenheit.isChecked = true
        }

        when (windUnit) {
            "km/h" -> binding.radioKmh.isChecked = true
            "mph" -> binding.radioMph.isChecked = true
        }

        // Handle temperature unit selection
        binding.radioGroupTempUnits.setOnCheckedChangeListener { _, checkedId ->
            val tempUnit = when (checkedId) {
                binding.radioCelsius.id -> "celsius"
                binding.radioFahrenheit.id -> "fahrenheit"
                else -> "celsius"
            }
            editor.putString("temp_unit", tempUnit).apply()
            Log.d("SettingsFragment", "Temperature unit set to $tempUnit")
        }

        // Handle wind speed unit selection
        binding.radioGroupUnits.setOnCheckedChangeListener { _, checkedId ->
            val windUnit = when (checkedId) {
                binding.radioKmh.id -> "km/h"
                binding.radioMph.id -> "mph"
                else -> "km/h"
            }
            editor.putString("wind_unit", windUnit).apply()
            Log.d("SettingsFragment", "Wind unit set to $windUnit")
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}