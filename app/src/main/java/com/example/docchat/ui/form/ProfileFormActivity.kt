package com.example.docchat.ui.form

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.docchat.R
import com.example.docchat.ui.login.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    private lateinit var progressDialog: AlertDialog

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
            lifecycleScope.launch {
                locationHelper.fetchCurrentLocation { location ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        locationSearchField.setText(location)
                        selectedLocation = location
                    }
                }
            }
        } else {
            Toast.makeText(this, "GPS permission is required to fetch location.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_form)

        setupProgressDialog()
        initializeFirebase()
        initializeViews()
        setupToolbar()
        setupListeners()

        // Request GPS permission
        checkGpsAndRequestPermission()
        setupLocationSearchField()
    }

    private fun setupProgressDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setView(R.layout.progress_dialog) // Buat layout XML dengan ProgressBar
        builder.setCancelable(false)
        progressDialog = builder.create()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        locationHelper = LocationHelper(this, mapPickerLauncher)
        datePickerHelper = DatePickerHelper(this)
        firebaseHelper = FirebaseHelper(this, auth)
    }

    private fun initializeViews() {
        datePickerButton = findViewById(R.id.datePickerButton)
        selectedDateTextView = findViewById(R.id.selectedDateTextView)
        mapButton = findViewById(R.id.mapButton)
        saveButton = findViewById(R.id.saveButton)
        locationSearchField = findViewById(R.id.locationSearchField)
    }

    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.mainToolbar))
        supportActionBar?.apply {
            title = "Profile Form"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupListeners() {
        datePickerButton.setOnClickListener { datePickerHelper.showDatePicker(selectedDateTextView) }
        mapButton.setOnClickListener { locationHelper.openMapPicker() }
        saveButton.setOnClickListener { saveUserProfile() }
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
        // Sign out from Firebase
        auth.signOut()

        // Optionally, sign out from Google as well if using Google Sign-In
        GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()

        // Navigate to LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // End MainActivity to prevent back navigation
        true
    }

    private fun setupLocationSearchField() {
        if (!locationHelper.isGpsEnabled()) {
            locationSearchField.error = "Enable GPS to fetch location"
            locationHelper.promptEnableGps()
        }

        locationSearchField.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = locationSearchField.compoundDrawablesRelative[2]
                if (drawableEnd != null && event.rawX >= (locationSearchField.right - drawableEnd.bounds.width())) {
                    // Handle GPS icon click
                    requestLocationFromGps()
                    // Call performClick to handle accessibility
                    locationSearchField.performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }

        locationSearchField.setOnClickListener {
            // Ensure performClick is recognized for accessibility
            Log.d("LocationSearchField", "EditText clicked")
        }
    }

    private fun checkGpsAndRequestPermission() {
        lifecycleScope.launch {
            locationHelper.fetchCurrentLocation { location ->
                lifecycleScope.launch(Dispatchers.Main) {
                    locationSearchField.setText(location)
                    selectedLocation = location
                }
            }
        }
    }

    private fun requestLocationFromGps() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            gpsPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        lifecycleScope.launch {
            locationHelper.fetchCurrentLocation { location ->
                lifecycleScope.launch(Dispatchers.Main) {
                    locationSearchField.setText(location)
                    selectedLocation = location
                }
            }
        }
    }

    private fun saveUserProfile() {
        val userId = auth.currentUser?.uid ?: return
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
        } else {
            progressDialog.show() // Menampilkan loading bar
            firebaseHelper.saveUserProfileToFirestore(
                userId,
                name,
                gender,
                phoneNumber,
                birthday,
                selectedLocation
            ) {
                // Callback setelah selesai
                progressDialog.dismiss() // Sembunyikan loading bar
                firebaseHelper.startPhoneVerification(phoneNumber, this)
            }
        }
    }

    companion object {
        const val GPS_PERMISSION_REQUEST_CODE = 2
    }
}
