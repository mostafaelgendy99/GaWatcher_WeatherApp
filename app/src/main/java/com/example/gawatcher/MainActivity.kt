package com.example.gawatcher

import android.os.Bundle
import android.util.Log
import android.view.Menu
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.NavigationUI
import com.example.gawatcher.databinding.ActivityMainBinding
import com.example.gawatcher.gendykey._apikey
import com.example.gawatcher.model.local.LocalDataSource
import com.example.gawatcher.model.local.WeatherDao
import com.example.gawatcher.model.local.WeatherDatabase
import com.example.gawatcher.model.remote.RemoteDataSource
import com.example.gawatcher.model.repo.DataRepo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    lateinit var  dataRepo : DataRepo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_favorites, R.id.nav_alerts , R.id.nav_settings , R.id.nav_map
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

         dataRepo =  DataRepo.getInstance(  RemoteDataSource() , LocalDataSource(WeatherDatabase.getDatabase(this).weatherDao()) )

        lifecycleScope.launch {
            val result = dataRepo.getWeatherForecast(44.34,10.99 , apiKey = _apikey  )
            if(result.isSuccess) {
                Log.i("gendyishere", "Success: ${result.getOrNull()}")

            }
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val bundle = Bundle().apply {
                        putDouble("latitude", 0.0)
                        putDouble("longitude", 0.0)
                    }
                    navController.navigate(R.id.nav_home, bundle)
                    drawerLayout.closeDrawers()
                    true
                }

                R.id.nav_map -> {
                    val bundle = Bundle().apply {
                        putString("senderId", "NavDrawer")
                    }
                    navController.navigate(R.id.nav_map, bundle)
                    drawerLayout.closeDrawers()
                    true
                }

                R.id.nav_favorites -> {
                    navController.navigate(R.id.nav_favorites)
                    drawerLayout.closeDrawers()
                    true
                }

                R.id.nav_alerts -> {
                    navController.navigate(R.id.nav_alerts)
                    drawerLayout.closeDrawers()
                    true
                }

                R.id.nav_settings -> {
                    navController.navigate(R.id.nav_settings)
                    drawerLayout.closeDrawers()
                    true
                }

                else -> false
            }
        }




    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    // Method to update the toolbar title from fragments
    fun updateToolbarTitle(title: String) {
        supportActionBar?.title = title
    }
}