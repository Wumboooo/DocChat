package com.example.docchat.ui.adminlist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.docchat.R
import com.example.docchat.ui.Admin
import com.example.docchat.ui.Doctor
import com.google.firebase.firestore.FirebaseFirestore

class EditUserDialog(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val existingUser: Any
) {

    fun show() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Edit Pengguna")

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_user, null)
        val emailEditText = view.findViewById<EditText>(R.id.emailEditText)
        val nameEditText = view.findViewById<EditText>(R.id.nameEditText)
        val tierRoleSpinner = view.findViewById<Spinner>(R.id.tierRoleSpinner)
        val specializationEditText = view.findViewById<EditText>(R.id.specializationEditText)
        val feeEditText = view.findViewById<EditText>(R.id.feeEditText)
        val experienceEditText = view.findViewById<EditText>(R.id.experienceEditText)
        val roleSpinner = view.findViewById<Spinner>(R.id.roleSpinner)

        // Default: Sembunyikan field spesialisasi, fee, dan pengalaman
        specializationEditText.visibility = View.GONE
        feeEditText.visibility = View.GONE
        experienceEditText.visibility = View.GONE

        emailEditText.isEnabled = false

        val tiers = arrayOf("basic", "master")
        val roles = arrayOf("Admin", "Dokter")
        val tiersAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, tiers)
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, roles)
        tierRoleSpinner.adapter = tiersAdapter
        roleSpinner.adapter = adapter

        if (existingUser is Admin) {
            emailEditText.setText(existingUser.email)
            nameEditText.setText(existingUser.name)
            roleSpinner.setSelection(0)
            tierRoleSpinner.setSelection(tiers.indexOf(existingUser.tier))
            tierRoleSpinner.visibility = View.VISIBLE
        } else if (existingUser is Doctor) {
            emailEditText.setText(existingUser.email)
            nameEditText.setText(existingUser.name)
            specializationEditText.setText(existingUser.specialization)
            feeEditText.setText(existingUser.fee.toString())
            experienceEditText.setText(existingUser.experience.toString())
            roleSpinner.setSelection(1)

            specializationEditText.visibility = View.VISIBLE
            feeEditText.visibility = View.VISIBLE
            experienceEditText.visibility = View.VISIBLE
            tierRoleSpinner.visibility = View.GONE
        }

        builder.setView(view)
        builder.setPositiveButton("Simpan") { _, _ ->
            val name = nameEditText.text.toString().trim()
            val tier = tierRoleSpinner.selectedItem.toString()

            if (name.isEmpty()) {
                Toast.makeText(context, "Nama tidak boleh kosong.", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (existingUser is Admin) {
                updateAdmin(existingUser.email, name, tier)
            } else if (existingUser is Doctor) {
                val specialization = specializationEditText.text.toString().trim()
                val fee = feeEditText.text.toString().toIntOrNull() ?: 0
                val experience = experienceEditText.text.toString().toIntOrNull() ?: 0

                updateDoctor(existingUser.email, name, specialization, fee, experience)
            }
        }
        builder.setNegativeButton("Batal", null)
        builder.create().show()
    }

    private fun updateAdmin(email: String, name: String, tier: String) {
        firestore.collection("admins").document(email)
            .update("name", name, "tier", tier)
            .addOnSuccessListener {
                Toast.makeText(context, "Admin berhasil diperbarui.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDoctor(email: String, name: String, specialization: String, fee: Int, experience: Int) {
        firestore.collection("doctors").document(email)
            .update(
                mapOf(
                    "name" to name,
                    "specialization" to specialization,
                    "fee" to fee,
                    "experience" to experience
                )
            )
            .addOnSuccessListener {
                Toast.makeText(context, "Dokter berhasil diperbarui.", Toast.LENGTH_SHORT).show()
            }
    }
}

