package com.example.docchat.form

import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatePickerHelper(private val activity: AppCompatActivity) {

    fun showDatePicker(selectedDateTextView: TextView) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Birthday")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds()) // Set current date as default
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDate))
            selectedDateTextView.text = formattedDate
        }

        datePicker.show(activity.supportFragmentManager, "DATE_PICKER")
    }
}

