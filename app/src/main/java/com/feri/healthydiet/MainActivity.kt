package com.feri.healthydiet

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.feri.healthydiet.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    // Launcher pentru solicitarea permisiunilor
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            Log.d(TAG, "All required permissions granted")
        } else {
            Log.d(TAG, "Some permissions were denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            Log.d("MainActivity", "onCreate: Start")
            super.onCreate(savedInstanceState)
            Log.d("MainActivity", "onCreate: After super.onCreate")

            binding = ActivityMainBinding.inflate(layoutInflater)
            Log.d("MainActivity", "onCreate: After binding initialization")
            setContentView(binding.root)
            Log.d("MainActivity", "onCreate: After setContentView")

            requestNeededPermissions()
            Log.d("MainActivity", "onCreate: After requestNeededPermissions")

            try {
                setupNavigation()
                Log.d("MainActivity", "onCreate: After setupNavigation")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error setting up navigation: ${e.message}", e)
            }
            Log.d("MainActivity", "onCreate: End")
        } catch (e: Exception) {
            Log.e("MainActivity", "Global error in onCreate: ${e.message}", e)
        }
    }

    private fun requestNeededPermissions() {
        Log.d(TAG, "Requesting permissions")
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        // Pe Android 13+ avem permisiuni diferite pentru accesul la imagini
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: ${permissionsToRequest.joinToString()}")
            permissionLauncher.launch(permissionsToRequest)
        } else {
            Log.d(TAG, "All permissions already granted")
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController

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
            navController.navigateUp() || super.onSupportNavigateUp()
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error: ${e.message}")
            super.onSupportNavigateUp()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}