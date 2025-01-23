package com.example.docchat.ui.form

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationHelper(
    private val activity: Activity,
    private val mapPickerLauncher: ActivityResultLauncher<Intent>
) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(activity)

    // Check if GPS is enabled
    fun isGpsEnabled(): Boolean {
        val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    // Prompt the user to enable GPS
    fun promptEnableGps() {
        AlertDialog.Builder(activity)
            .setMessage("GPS is required for this feature. Do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Fetch the current location
    suspend fun fetchCurrentLocation(callback: (String) -> Unit) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                ProfileFormActivity.GPS_PERMISSION_REQUEST_CODE
            )
            return
        }

        withContext(Dispatchers.IO) {
            val location = fusedLocationClient.lastLocation.await() // Requires `kotlinx-coroutines-play-services`
            val address = if (location != null) {
                val geocoder = Geocoder(activity, Locale.getDefault())
                geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    ?.get(0)
                    ?.getAddressLine(0) ?: "Unable to fetch address"
            } else {
                "Location not available"
            }
            callback(address)
        }
    }


    fun checkAndRequestGpsPermission(onGranted: () -> Unit) {
        if (!isGpsEnabled()) {
            promptEnableGps()
        } else if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                ProfileFormActivity.GPS_PERMISSION_REQUEST_CODE
            )
        } else {
            onGranted()
        }
    }

    // Open the map picker activity
    fun openMapPicker() {
        val intent = Intent(activity, MapPickerActivity::class.java)
        mapPickerLauncher.launch(intent)
    }
}

