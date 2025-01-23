package com.example.docchat

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.docchat.ui.form.ProfileFormActivity
import com.example.docchat.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class SplashScreenActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)

        // Set the navigation bar color
        window.navigationBarColor = resources.getColor(R.color.darkerblue, theme)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // Check if the user is logged in
        if (currentUser != null) {
            checkUserProfile(currentUser)
        } else {
            navigateTo(LoginActivity::class.java)
        }
    }

    private fun checkUserProfile(user: FirebaseUser) {
        val email = user.email ?: return navigateTo(LoginActivity::class.java)

        // Query untuk memeriksa apakah email sudah ada di Firestore
        firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val profileCompleted = document.getBoolean("profileCompleted") ?: false

                    if (profileCompleted) {
                        // Jika email ditemukan dan profileCompleted = true, ke MainActivity
                        navigateTo(MainActivity::class.java)
                    } else {
                        // Jika profileCompleted = false, ke ProfileFormActivity
                        navigateTo(ProfileFormActivity::class.java)
                    }
                } else {
                    // Jika email tidak ditemukan, ke ProfileFormActivity
                    navigateTo(ProfileFormActivity::class.java)
                }
            }
            .addOnFailureListener {
                Log.e("SplashScreenActivity", "Error checking email existence", it)
                navigateTo(LoginActivity::class.java)
            }
    }

    private fun navigateTo(destination: Class<*>) {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, destination)
            startActivity(intent)
            finish()
        }, 2000)
    }
}
