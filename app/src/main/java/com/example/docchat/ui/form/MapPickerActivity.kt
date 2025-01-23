package com.example.docchat.ui.form

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.docchat.R
import com.example.docchat.ui.form.ProfileFormActivity.Companion.GPS_PERMISSION_REQUEST_CODE
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

class MapPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var selectedMarker: Marker? = null
    private lateinit var confirmLocationButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_picker)

        // Initialize the map
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), GPS_PERMISSION_REQUEST_CODE)
        } else {
            val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val userLocation = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12f))
                    selectedMarker?.remove()
                    selectedMarker = googleMap.addMarker(MarkerOptions().position(userLocation).title("Your Location"))
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to fetch current location.", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize confirm button
        confirmLocationButton = findViewById(R.id.confirmLocationButton)
        confirmLocationButton.setOnClickListener {
            val latLng = selectedMarker?.position
            if (latLng != null) {
                val geocoder = Geocoder(this, Locale.getDefault())
                try {
                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                    val address = addresses?.get(0)?.getAddressLine(0) ?: "Unknown Location"
                    val resultIntent = Intent().apply {
                        putExtra("selected_address", address)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to fetch address: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please select a location on the map.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Get the default location
        val defaultLatitude = intent.getDoubleExtra("default_latitude", -6.2088) // Jakarta latitude
        val defaultLongitude = intent.getDoubleExtra("default_longitude", 106.8456) // Jakarta longitude

        val defaultLocation = LatLng(defaultLatitude, defaultLongitude)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))

        // Add marker for default location
        selectedMarker = googleMap.addMarker(MarkerOptions().position(defaultLocation).title("Default Location"))

        // Set map click listener to update marker position
        googleMap.setOnMapClickListener { latLng ->
            selectedMarker?.remove()
            selectedMarker = googleMap.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
        }
    }
}
