package com.example.docchat

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.docchat.login.LoginActivity

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        // Set the navigation bar color
        window.navigationBarColor = resources.getColor(R.color.darkerblue, theme)

        // Delay for 2 seconds before moving to LoginActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()  // Finish SplashActivity so it's removed from the back stack
        }, 2000) // Delay time in milliseconds (2000ms = 2 seconds)
    }
}