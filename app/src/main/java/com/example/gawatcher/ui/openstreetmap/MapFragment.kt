package com.example.gawatcher.ui.openstreetmap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.gawatcher.databinding.FragmentMapBinding
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.events.MapEventsReceiver
import androidx.navigation.fragment.findNavController
import com.example.gawatcher.R
import com.example.gawatcher.model.local.LocalDataSource
import com.example.gawatcher.model.local.WeatherDatabase
import com.example.gawatcher.model.remote.RemoteDataSource
import com.example.gawatcher.model.repo.DataRepo
import com.example.gawatcher.ui.home.HomeViewModel
import com.example.gawatcher.ui.home.HomeViewModelFactory


class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private var selectedLat: Double? = null
    private var selectedLon: Double? = null
    private lateinit var mapView: MapView
    private lateinit var senderId: String
    private val viewModel: MapViewModel by viewModels {
        MapViewModelFactory(
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
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        val context = requireContext()
        Configuration.getInstance().load(context, context.getSharedPreferences("osm_prefs", 0))

        mapView = binding.mapView
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(10.0)
        val defaultPoint = GeoPoint(30.0444, 31.2357) // Default to Cairo
        mapView.controller.setCenter(defaultPoint)

        // Map tap event listener
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false

            override fun longPressHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    selectedLat = p.latitude
                    selectedLon = p.longitude

                    // Clear previous markers
                    mapView.overlays.removeIf { it is Marker }

                    // Add new marker
                    val marker = Marker(mapView).apply {
                        position = p
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Selected Location"
                    }
                    mapView.overlays.add(marker)
                    mapView.invalidate()

                    Toast.makeText(context, "Location Selected", Toast.LENGTH_SHORT).show()
                }
                return true
            }
        }

        val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
        mapView.overlays.add(mapEventsOverlay)
//

        // Handle Select Location button click
        if(senderId == "FavoritesFragment") {
            binding.btnSelectLocation.text = "Add to Favorites"
            binding.btnSelectLocation.setOnClickListener{
                if (selectedLat != null && selectedLon != null) {
                    viewModel.insertLocation(selectedLat!!,selectedLon!!)
                    viewModel.insertedStatus.observe(viewLifecycleOwner){value->
                        if(value == false){
                            Toast.makeText(context, "Can't insert location", Toast.LENGTH_SHORT).show()
                        }
                        else{
                            Log.i("MapFragment", "Location inserted successfully")
                            findNavController().navigate(R.id.nav_favorites)
                        }
                    }
                } else {
                    Toast.makeText(context, "Please long-press on the map to select a location", Toast.LENGTH_SHORT).show()
                }
            }
        }
        else {
            binding.btnSelectLocation.setOnClickListener {
                if (selectedLat != null && selectedLon != null) {
                    val bundle = Bundle().apply {
                        putDouble("latitude", selectedLat!!)
                        putDouble("longitude", selectedLon!!)
                    }
                    findNavController().navigate(R.id.nav_home, bundle)
                } else {
                    Toast.makeText(context, "Please long-press on the map to select a location", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
