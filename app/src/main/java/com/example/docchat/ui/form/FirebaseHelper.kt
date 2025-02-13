package com.example.docchat.ui.form

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.docchat.ui.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FirebaseHelper(private val context: Context, private val auth: FirebaseAuth) {

    fun saveUserProfileToFirestore(
        email: String,
        name: String,
        gender: String,
        phoneNumber: String,
        birthday: String,
        location: String,
        isProfileCompleted: Boolean,
        onComplete: () -> Unit // Callback){}
    ) {
        val db = FirebaseFirestore.getInstance()
        val userData = mapOf(
            "name" to name,
            "gender" to gender,
            "phone" to phoneNumber,
            "birthday" to birthday,
            "location" to location,
            "isProfileCompleted" to isProfileCompleted
        )

        db.collection("users").document(email)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(context, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                val intent = Intent(context, MainActivity::class.java) // lewati phone verification
                context.startActivity(intent)
                onComplete() // Panggil callback setelah selesai
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to save profile: ${e.message}", Toast.LENGTH_SHORT).show()
                val intent = Intent(context, ProfileFormActivity::class.java) // lewati phone verification
                context.startActivity(intent)
                onComplete() // Tetap panggil callback untuk sembunyikan loading
            }
    }
}

