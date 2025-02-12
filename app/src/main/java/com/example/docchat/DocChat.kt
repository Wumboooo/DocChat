package com.example.docchat

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class DocChat : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        instance = this
    }

    companion object {
        lateinit var instance: DocChat
            private set
    }
}