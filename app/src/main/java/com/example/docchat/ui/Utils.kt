package com.example.docchat.ui

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager

object Utils {
    fun dismissKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = activity.currentFocus
        view?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
}