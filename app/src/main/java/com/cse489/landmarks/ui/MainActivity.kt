package com.cse489.landmarks.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cse489.landmarks.R
import com.cse489.landmarks.databinding.ActivityMainBinding
import com.cse489.landmarks.ui.activity.ActivityFragment
import com.cse489.landmarks.ui.add.AddViewFragment
import com.cse489.landmarks.ui.list.LandmarksFragment
import com.cse489.landmarks.ui.map.MapFragment
import com.cse489.landmarks.ui.viewmodel.LandmarkViewModel
import com.cse489.landmarks.ui.viewmodel.LandmarkViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: LandmarkViewModel by viewModels { LandmarkViewModelFactory(application) }

    private val mapFragment by lazy { MapFragment() }
    private val landmarksFragment by lazy { LandmarksFragment() }
    private val activityFragment by lazy { ActivityFragment() }
    private val addViewFragment by lazy { AddViewFragment() }

    private var activeFragment: Fragment = mapFragment

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (!granted) {
                Toast.makeText(this, "Location permission recommended for landmark visits", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        observeViewModel()
        requestLocationPermissions()
    }

    private fun setupNavigation() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, addViewFragment, "4").hide(addViewFragment)
            .add(R.id.fragment_container, activityFragment, "3").hide(activityFragment)
            .add(R.id.fragment_container, landmarksFragment, "2").hide(landmarksFragment)
            .add(R.id.fragment_container, mapFragment, "1")
            .commit()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map -> {
                    switchFragment(mapFragment)
                    true
                }
                R.id.nav_landmarks -> {
                    switchFragment(landmarksFragment)
                    true
                }
                R.id.nav_activity -> {
                    switchFragment(activityFragment)
                    true
                }
                R.id.nav_add_view -> {
                    switchFragment(addViewFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(targetFragment: Fragment) {
        if (activeFragment != targetFragment) {
            supportFragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(targetFragment)
                .commit()
            activeFragment = targetFragment
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.statusMessage.observe(this) { event ->
            event.getContentIfNotHandled()?.let { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.errorMessage.observe(this) { event ->
            event.getContentIfNotHandled()?.let { error ->
                AlertDialog.Builder(this)
                    .setTitle("Notice")
                    .setMessage(error)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun requestLocationPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        locationPermissionLauncher.launch(permissions.toTypedArray())
    }
}
