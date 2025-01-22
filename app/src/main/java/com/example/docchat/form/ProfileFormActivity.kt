package com.example.docchat.form

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.docchat.R
import com.example.docchat.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileFormActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var locationHelper: LocationHelper
    private lateinit var datePickerHelper: DatePickerHelper
    private lateinit var firebaseHelper: FirebaseHelper

    private lateinit var datePickerButton: Button
    private lateinit var selectedDateTextView: TextView
    private lateinit var mapButton: Button
    private lateinit var saveButton: Button

    private lateinit var locationSearchField: EditText
    private lateinit var selectedLocation: String

    private val mapPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val selectedLocation = data?.getStringExtra("selected_address") ?: "No location selected"
            locationSearchField.setText(selectedLocation)
            this.selectedLocation = selectedLocation
        }
    }

    private val gpsPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            locationHelper.fetchCurrentLocation { location ->
                locationSearchField.setText(location) // Set GPS location address
                selectedLocation = location
            }
        } else {
            Toast.makeText(this, "GPS permission is required to fetch location.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_form)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()
        locationHelper = LocationHelper(this, mapPickerLauncher)
        datePickerHelper = DatePickerHelper(this)
        firebaseHelper = FirebaseHelper(this, auth)

        // Set up Action Bar with back button
        supportActionBar?.apply {
            title = "Profile Form"
            setDisplayHomeAsUpEnabled(true)
        }

        // Initialize views
        datePickerButton = findViewById(R.id.datePickerButton)
        selectedDateTextView = findViewById(R.id.selectedDateTextView)
        mapButton = findViewById(R.id.mapButton)
        saveButton = findViewById(R.id.saveButton)
        locationSearchField = findViewById(R.id.locationSearchField)

        // Set up listeners
        datePickerButton.setOnClickListener { datePickerHelper.showDatePicker(selectedDateTextView) }
        mapButton.setOnClickListener { locationHelper.openMapPicker() }
        saveButton.setOnClickListener { saveUserProfile() }

        // Check and request GPS permission
        if (!locationHelper.isGpsEnabled()) {
            locationHelper.promptEnableGps()
        } else {
            gpsPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Handle back button press to log out and clear session
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                logOutAndClearSession()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logOutAndClearSession() {
        // Log out from Firebase
        auth.signOut()

        // Clear shared preferences or any other session data
        val preferences = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.clear()
        editor.apply()

        // Navigate to the login screen or start a new activity
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Close the current activity
    }

    private fun saveUserProfile() {
        val name = findViewById<EditText>(R.id.nameEditText).text.toString().trim()
        val phoneNumber = findViewById<EditText>(R.id.phoneNumberEditText).text.toString().trim()

        // Validasi format nomor telepon
        val phoneNumberPattern = "^(\\+62|0)\\d{8,12}$"
        val regex = Regex(phoneNumberPattern)
        if (!regex.matches(phoneNumber)) {
            Toast.makeText(this, "Please enter a valid phone number (e.g., +62XXXXXXXXXX)", Toast.LENGTH_SHORT).show()
            return
        }

        val gender = when (findViewById<RadioGroup>(R.id.rgGender).checkedRadioButtonId) {
            R.id.rbMale -> "Male"
            R.id.rbFemale -> "Female"
            else -> "Not specified"
        }

        val birthday = selectedDateTextView.text.toString().takeIf { it != "No date selected" } ?: "Not specified"

        if (name.isEmpty() || phoneNumber.isEmpty() || gender.isEmpty() || birthday == "Not specified" || selectedLocation.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        firebaseHelper.startPhoneVerification(phoneNumber, name, gender, birthday, selectedLocation, this)
    }

    companion object {
        const val GPS_PERMISSION_REQUEST_CODE = 2
    }
}
