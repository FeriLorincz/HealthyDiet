package com.feri.healthydiet

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.feri.healthydiet.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setarea navigării dacă avem bottom navigation
       // setupNavigation()

        try {
            setupNavigation()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up navigation: ${e.message}")
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.dashboardFragment, R.id.menuScanFragment,
                R.id.foodAnalyzerFragment, R.id.historyFragment, R.id.profileFragment
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return try {
            val navController = findNavController(R.id.nav_host_fragment_activity_main)
            navController.navigateUp() || super.onSupportNavigateUp()
        } catch (e: Exception) {
            Log.e("MainActivity", "Navigation error: ${e.message}")
            super.onSupportNavigateUp()
        }
    }
}