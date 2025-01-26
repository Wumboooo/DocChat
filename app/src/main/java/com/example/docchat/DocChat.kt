package com.example.docchat

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp

class DocChat : Application() {
    override fun onCreate() {
        super.onCreate()
        // Set the default night mode for the entire app
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        instance = this
    }

    companion object {
        lateinit var instance: DocChat
            private set
    }

}