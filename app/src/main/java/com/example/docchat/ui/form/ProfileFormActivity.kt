package com.example.docchat.ui.form

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ProfileFormActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var locationHelper: LocationHelper
    private lateinit var datePickerHelper: DatePickerHelper
    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var locationSearchField: EditText
    private lateinit var progressDialog: AlertDialog

    private val mapPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra("selected_address")?.let {
                locationSearchField.setText(it)
            }
        }
    }

    private val gpsPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) fetchCurrentLocation() else showToast("GPS permission is required to fetch location.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_form)

        initializeUI()
        initializeHelpers()
        setupLocationHandling()

        val isEditMode = intent.getBooleanExtra("edit_mode", false)
        if (isEditMode) {
            loadUserProfile()
        }
    }

    private fun loadUserProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(currentUser.email!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    findViewById<EditText>(R.id.nameEditText).setText(document.getString("name") ?: "")
                    findViewById<EditText>(R.id.phoneNumberEditText).setText(document.getString("phone") ?: "")
                    findViewById<TextView>(R.id.selectedDateTextView).text = document.getString("birthday") ?: "Pilih Tanggal Lahir"
                    findViewById<EditText>(R.id.locationSearchField).setText(document.getString("location") ?: "")

                    val gender = document.getString("gender") ?: ""
                    when (gender) {
                        "Male" -> findViewById<RadioButton>(R.id.rbMale).isChecked = true
                        "Female" -> findViewById<RadioButton>(R.id.rbFemale).isChecked = true
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat profil", Toast.LENGTH_SHORT).show()
            }
    }


    private fun initializeUI() {
        setupProgressDialog()
        setupToolbar()
        setupListeners()
        setupLocationField()

        // Dismiss keyboard when tapping outside EditText
        findViewById<View>(R.id.main).setOnTouchListener { _, _ ->
            dismissKeyboard()
            false
        }
    }

    private fun initializeHelpers() {
        auth = FirebaseAuth.getInstance()
        locationHelper = LocationHelper(this, mapPickerLauncher)
        datePickerHelper = DatePickerHelper(this)
        firebaseHelper = FirebaseHelper(this, auth)
    }

    private fun setupProgressDialog() {
        progressDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_progress)
            .setCancelable(false)
            .create()
    }

    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.mainToolbar))
        supportActionBar?.apply {
            title = "Profile Form"
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.baseline_arrow_back_24) // White arrow icon
        }
    }

    private fun setupListeners() {
        findViewById<Button>(R.id.datePickerButton).setOnClickListener {
            datePickerHelper.showDatePicker(findViewById(R.id.selectedDateTextView))
        }
        findViewById<Button>(R.id.mapButton).setOnClickListener {
            locationHelper.openMapPicker()
        }
        findViewById<Button>(R.id.saveButton).setOnClickListener {
            saveUserProfile()
        }
    }

    private fun setupLocationField() {
        locationSearchField = findViewById(R.id.locationSearchField)
        locationSearchField.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP && isDrawableEndClicked(event, locationSearchField)) {
                requestLocationFromGps()
                true
            } else false
        }
    }

    private fun setupLocationHandling() {
        if (!locationHelper.isGpsEnabled()) {
            locationHelper.promptEnableGps()
        } else {
            fetchCurrentLocation()
        }
    }

    private fun requestLocationFromGps() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            gpsPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else fetchCurrentLocation()
    }

    private fun fetchCurrentLocation() {
        lifecycleScope.launch {
            locationHelper.fetchCurrentLocation { location ->
                runOnUiThread {
                    locationSearchField.setText(location)
                }
            }
        }
    }

    private fun saveUserProfile() {
        val currentUser = auth.currentUser ?: return
        val currentEmail = currentUser.email ?: return
        val name = findViewById<EditText>(R.id.nameEditText).text.toString().trim()
        val phone = findViewById<EditText>(R.id.phoneNumberEditText).text.toString().trim()
        val gender = when (findViewById<RadioGroup>(R.id.rgGender).checkedRadioButtonId) {
            R.id.rbMale -> "Male"
            R.id.rbFemale -> "Female"
            else -> "Not specified"
        }
        val birthday = findViewById<TextView>(R.id.selectedDateTextView).text.toString().trim()
        val location = findViewById<EditText>(R.id.locationSearchField).text.toString().trim()

        if (!validateFields(name, phone, gender, birthday, location)) return

        progressDialog.show()
        firebaseHelper.saveUserProfileToFirestore(currentEmail, name, gender, phone, birthday, location, true) {
//            firebaseHelper.startPhoneVerification(phone, this, object : FirebaseHelper.PhoneVerificationCallback {
//                override fun onVerificationComplete() {
//                    progressDialog.dismiss()
//                    showToast("Profile saved and phone verified successfully")
//                }
//
//                override fun onVerificationFailed(error: String) {
//                    progressDialog.dismiss()
//                    showToast("Verification failed: $error")
//                }
//            })
        }
    }

    private fun validateFields(name: String, phone: String, gender: String, birthday: String, location: String): Boolean {
        val phonePattern = "^(\\+62|0)\\d{8,12}$".toRegex()

        return when {
            name.isEmpty() || phone.isEmpty() || gender == "Not specified" || birthday.isEmpty() || birthday == "Pilih Tanggal Lahir"|| location.isEmpty() || location == "Gagal memindai lokasi" || location == "Unable to fetch address" -> {
                showToast("Please fill all fields.")
                false
            }
            !phone.matches(phonePattern) -> {
                showToast("Invalid phone number format.")
                false
            }
            else -> true
        }
    }

    private fun isDrawableEndClicked(event: MotionEvent, field: EditText): Boolean {
        val drawableEnd = field.compoundDrawablesRelative[2]
        return drawableEnd != null && event.rawX >= (field.right - drawableEnd.bounds.width())
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            signOut()
            true
        } else super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Show confirmation dialog
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Are you sure you want to go back?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                super.onBackPressed()
                auth.signOut()
                GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }

        val alert = builder.create()
        alert.show()
    }

    private fun signOut() {
        auth.signOut()
        GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun dismissKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    companion object{
        const val GPS_PERMISSION_REQUEST_CODE = 1001
    }
}

