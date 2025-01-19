package com.example.docchat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode

class ProfileFormActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()

    private val PLACE_PICKER_REQUEST = 1

    private lateinit var datePicker: DatePicker
    private lateinit var datePickerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_form)

        auth = FirebaseAuth.getInstance()

        datePicker = findViewById(R.id.datePicker)
        datePickerButton = findViewById(R.id.datePickerButton)

        // Mengatur button untuk menampilkan/menyembunyikan DatePicker
        datePickerButton.setOnClickListener {
            if (datePicker.visibility == View.GONE) {
                datePicker.visibility = View.VISIBLE
                datePickerButton.text = "Hide Birthday Picker"
            } else {
                datePicker.visibility = View.GONE
                datePickerButton.text = "Select Birthday"
            }
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            saveUserProfile()
        }

        findViewById<Button>(R.id.locationEditText).setOnClickListener {
            openLocationPicker()
        }
    }

    private fun saveUserProfile() {
        val name = findViewById<EditText>(R.id.nameEditText).text.toString().trim()
        val phoneNumber = findViewById<EditText>(R.id.phoneNumberEditText).text.toString().trim()

        val genderRadioGroup: RadioGroup = findViewById(R.id.rgGender)
        val selectedGenderId = genderRadioGroup.checkedRadioButtonId
        val selectedGender = when (selectedGenderId) {
            R.id.rbMale -> "Male"
            R.id.rbFemale -> "Female"
            else -> "Not specified"
        }

        val birthday = if (datePicker.visibility == View.VISIBLE) {
            val day = datePicker.dayOfMonth
            val month = datePicker.month + 1 // DatePicker months are 0-based
            val year = datePicker.year
            "$day/$month/$year"
        } else {
            "Not specified"
        }

        val location = findViewById<EditText>(R.id.locationEditText).text.toString().trim()

        if (name.isEmpty() || phoneNumber.isEmpty() || selectedGender.isEmpty() || birthday.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val userProfile = mapOf(
            "name" to name,
            "phoneNumber" to phoneNumber,
            "gender" to selectedGender,
            "birthday" to birthday,
            "location" to location
        )

        auth.currentUser?.let { user ->
            firestore.collection("users").document(user.uid)
                .update(userProfile)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Fungsi untuk membuka lokasi picker
    private fun openLocationPicker() {
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)

        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
            .build(this)

        startActivityForResult(intent, PLACE_PICKER_REQUEST)
    }
}

