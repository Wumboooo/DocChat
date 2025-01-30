package com.example.docchat.ui.chat

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.example.docchat.R
import com.example.docchat.ui.Utils
import com.google.firebase.firestore.DocumentSnapshot

object ChatHelper {
    fun showDoctorSelectionDialog(
        context: Context,
        doctorList: List<DocumentSnapshot>,
        onDoctorSelected: (String) -> Unit
    ) {
        val doctorNames = doctorList.map { doc ->
            val name = doc.getString("name") ?: "Unknown"
            val specialization = doc.getString("specialization") ?: "Unknown"
            "$name - $specialization"
        }

        AlertDialog.Builder(context)
            .setTitle("Pilih Dokter")
            .setItems(doctorNames.toTypedArray()) { _, which ->
                val selectedDoctor = doctorList[which]
                onDoctorSelected(selectedDoctor.id)
            }
            .show()
    }

    fun showSummaryDialog(
        activity: Activity,
        onSave: (String, String) -> Unit
    ) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_summary, null)
        val diseaseEditText = dialogView.findViewById<EditText>(R.id.diseaseEditText)
        val medicineEditText = dialogView.findViewById<EditText>(R.id.medicineEditText)

        val dialog = AlertDialog.Builder(activity)
            .setTitle("Buat Ringkasan")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val disease = diseaseEditText.text.toString()
                val medicine = medicineEditText.text.toString()
                if (disease.isBlank() || medicine.isBlank()) {
                    Toast.makeText(activity, "Harap isi semua field.", Toast.LENGTH_SHORT).show()
                } else {
                    onSave(disease, medicine)
                }
            }
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            val view = dialogView.findViewById<View>(R.id.summaryLayout) ?: dialogView
            view.setOnTouchListener { _, _ ->
                Utils.dismissKeyboard(activity)
                false
            }
        }

        dialog.show()
    }
}


