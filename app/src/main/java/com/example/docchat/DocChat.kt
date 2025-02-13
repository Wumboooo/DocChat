package com.example.docchat

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.docchat.ui.MyFirebaseMessagingService.Companion.createNotificationChannel

class DocChat : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        instance = this
        createNotificationChannel(this)
    }

    companion object {
        lateinit var instance: DocChat
            private set
    }
}