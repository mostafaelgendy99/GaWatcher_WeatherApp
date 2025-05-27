package com.example.gawatcher.ui.favorites

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gawatcher.R
import com.example.gawatcher.databinding.FragmentFavoritesBinding
import com.example.gawatcher.model.local.LocalDataSource
import com.example.gawatcher.model.local.WeatherDatabase
import com.example.gawatcher.model.remote.RemoteDataSource
import com.example.gawatcher.model.repo.DataRepo
import com.example.gawatcher.ui.openstreetmap.MapViewModel
import com.example.gawatcher.ui.openstreetmap.MapViewModelFactory

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val favoritesViewModel: FavoritesViewModel by viewModels {
        FavoritesViewModelFactory(
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


        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        val root: View = binding.root
// Set up RecyclerView

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = FavoritesAdapter(
            onDeleteClick = { id ->
                favoritesViewModel.deleteFavorite(id)
            },
            onItemClick = { weatherEntity ->
                weatherEntity.currentWeather?.coord?.let { coord ->
                    val bundle = Bundle().apply {
                        putDouble("latitude", coord.lat)
                        putDouble("longitude", coord.lon)
                    }
                    findNavController().navigate(R.id.nav_home, bundle)
                } ?: Toast.makeText(context, "Location data unavailable", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvFavorites.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }

        favoritesViewModel.fetchFavorites()

        favoritesViewModel.favorites.observe(viewLifecycleOwner) { result ->
            result.onSuccess { favorites ->
                adapter.submitList(favorites)
                binding.rvFavorites.visibility = if (favorites.isEmpty()) View.GONE else View.VISIBLE
                binding.tvEmptyMessage.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
            }.onFailure {
                binding.rvFavorites.visibility = View.GONE
                binding.tvEmptyMessage.text = it.message
                binding.tvEmptyMessage.visibility = View.VISIBLE
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnAddFromMap.setOnClickListener {
            val senderId = "FavoritesFragment"
            val bundle = Bundle().apply {
                putString("sender_id", senderId)
            }
            findNavController().navigate(R.id.nav_map, bundle)
        }
        Log.i("FavoritesFragment", "onViewCreated called")

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}