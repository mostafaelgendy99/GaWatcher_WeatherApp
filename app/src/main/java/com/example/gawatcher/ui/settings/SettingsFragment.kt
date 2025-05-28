package com.example.gawatcher.ui.settings

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.gawatcher.databinding.FragmentSettingsBinding
import java.util.Locale

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
        if (!sharedPrefs.contains("language")) {
            editor.putString("language", "english").apply()
            Log.d("SettingsFragment", "Set default language to english")
        }

        // Load saved preferences
        val tempUnit = sharedPrefs.getString("temp_unit", "celsius")?.lowercase() ?: "celsius"
        val windUnit = sharedPrefs.getString("wind_unit", "km/h")?.lowercase() ?: "km/h"
        val language = sharedPrefs.getString("language", "english")?.lowercase() ?: "english"

        // Set initial radio button states
        when (tempUnit) {
            "celsius" -> binding.radioCelsius.isChecked = true
            "fahrenheit" -> binding.radioFahrenheit.isChecked = true
        }
        when (windUnit) {
            "km/h" -> binding.radioKmh.isChecked = true
            "mph" -> binding.radioMph.isChecked = true
        }
        when (language) {
            "english" -> binding.radioEnglish.isChecked = true
            "arabic" -> binding.radioArabic.isChecked = true
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

        // Handle language selection
        binding.languageGroup.setOnCheckedChangeListener { _, checkedId ->
            val language = when (checkedId) {
                binding.radioEnglish.id -> "english"
                binding.radioArabic.id -> "arabic"
                else -> "english"
            }
            editor.putString("language", language).apply()
            Log.d("SettingsFragment", "Language set to $language")
            updateLocale(language)
        }

        return binding.root
    }

    private fun updateLocale(language: String) {
        val locale = when (language) {
            "arabic" -> Locale("ar")
            else -> Locale("en")
        }
        Locale.setDefault(locale)
        val config = Configuration().apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        requireContext().resources.updateConfiguration(config, requireContext().resources.displayMetrics)
        // Restart activity to apply locale changes
        requireActivity().recreate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}