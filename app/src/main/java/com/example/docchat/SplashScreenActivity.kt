package com.example.docchat

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.docchat.ui.MainActivity
import com.example.docchat.ui.form.ProfileFormActivity
import com.example.docchat.ui.login.LoginActivity
import com.example.docchat.ui.login.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashScreenActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        window.navigationBarColor = resources.getColor(R.color.darkerblue, theme)

        auth = FirebaseAuth.getInstance()
        userRepository = UserRepository(FirebaseFirestore.getInstance())



        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = auth.currentUser
            if (currentUser != null) {
                userRepository.checkUserRole(currentUser.email.orEmpty()) { role ->
                    globalRole = role
                    when (role) {
                        "admin", "doctor" -> navigateTo(MainActivity::class.java)
                        "user" -> userRepository.isProfileCompleted(currentUser.email.orEmpty()) { isCompleted ->
                            if (isCompleted) {
                                navigateTo(MainActivity::class.java)
                            } else {
                                navigateTo(ProfileFormActivity::class.java)
                            }
                        }
                        else -> navigateTo(LoginActivity::class.java)
                    }
                }
            } else {
                navigateTo(LoginActivity::class.java)
            }
        }, 2000)
    }

    private fun navigateTo(activity: Class<*>) {
        startActivity(Intent(this, activity))
        finish()
    }

    companion object {
        var globalRole: String = ""
    }
}

