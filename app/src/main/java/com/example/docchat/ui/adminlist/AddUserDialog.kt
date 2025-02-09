package com.example.docchat.ui.adminlist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.docchat.R
import com.google.firebase.firestore.FirebaseFirestore


class AddUserDialog(
    private val context: Context,
    private val firestore: FirebaseFirestore
) {

    fun show() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Tambah Admin atau Dokter")

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_user, null)
        val emailEditText = view.findViewById<EditText>(R.id.emailEditText)
        val nameEditText = view.findViewById<EditText>(R.id.nameEditText)
        val tierRoleSpinner = view.findViewById<Spinner>(R.id.tierRoleSpinner)
        val specializationEditText = view.findViewById<EditText>(R.id.specializationEditText)
        val feeEditText = view.findViewById<EditText>(R.id.feeEditText)
        val experienceEditText = view.findViewById<EditText>(R.id.experienceEditText)
        val roleSpinner = view.findViewById<Spinner>(R.id.roleSpinner)

        specializationEditText.visibility = View.GONE
        feeEditText.visibility = View.GONE
        experienceEditText.visibility = View.GONE

        val tiers = arrayOf("basic", "master")
        val roles = arrayOf("Admin", "Dokter")
        val tiersAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, tiers)
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, roles)
        tierRoleSpinner.adapter = tiersAdapter
        roleSpinner.adapter = adapter

        roleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedRole = roles[position]
                if (selectedRole == "Admin") {
                    tierRoleSpinner.visibility = View.VISIBLE
                    specializationEditText.visibility = View.GONE
                    feeEditText.visibility = View.GONE
                    experienceEditText.visibility = View.GONE
                } else {
                    tierRoleSpinner.visibility = View.GONE
                    specializationEditText.visibility = View.VISIBLE
                    feeEditText.visibility = View.VISIBLE
                    experienceEditText.visibility = View.VISIBLE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        builder.setView(view)
        builder.setPositiveButton("Tambah") { _, _ ->
            val email = emailEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()
            val tier = tierRoleSpinner.selectedItem.toString()
            val role = roleSpinner.selectedItem.toString()

            if (email.isEmpty() || name.isEmpty()) {
                Toast.makeText(context, "Email dan nama harus diisi.", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            validateEmail(email) { isUnique ->
                if (isUnique) {
                    if (role == "Dokter") {
                        val specialization = specializationEditText.text.toString().trim()
                        val feeText = feeEditText.text.toString().trim()
                        val experienceText = experienceEditText.text.toString().trim()

                        if (specialization.isEmpty() || feeText.isEmpty() || experienceText.isEmpty()) {
                            Toast.makeText(context, "Semua field dokter harus diisi.", Toast.LENGTH_SHORT).show()
                            return@validateEmail
                        }

                        val fee = feeText.toIntOrNull() ?: 0
                        val experience = experienceText.toIntOrNull() ?: 0

                        addDoctorToDatabase(email, name, specialization, fee, experience)
                    } else {
                        addAdminToDatabase(email, name, tier)
                    }
                } else {
                    Toast.makeText(context, "Email sudah digunakan.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("Batal", null)
        builder.create().show()
    }

    private fun validateEmail(email: String, callback: (Boolean) -> Unit) {
        val adminsRef = firestore.collection("admins").document(email)
        val doctorsRef = firestore.collection("doctors").document(email)

        adminsRef.get().addOnSuccessListener { adminDoc ->
            if (adminDoc.exists()) {
                callback(false) // Email sudah digunakan di koleksi admins
            } else {
                doctorsRef.get().addOnSuccessListener { doctorDoc ->
                    callback(!doctorDoc.exists()) // Email tersedia jika tidak ada di koleksi doctors
                }.addOnFailureListener {
                    callback(false) // Error saat membaca Firestore
                }
            }
        }.addOnFailureListener {
            callback(false) // Error saat membaca Firestore
        }
    }


    private fun addAdminToDatabase(email: String, name: String, tier: String) {
        firestore.collection("admins").document(email)
            .set(mapOf("name" to name, "tier" to tier))
            .addOnSuccessListener {
                Toast.makeText(context, "Admin berhasil ditambahkan.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Gagal menambahkan Admin: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addDoctorToDatabase(email: String, name: String, specialization: String, fee: Int, experience: Int) {
        firestore.collection("doctors").document(email)
            .set(
                mapOf(
                    "name" to name,
                    "specialization" to specialization,
                    "fee" to fee,
                    "experience" to experience
                )
            )
            .addOnSuccessListener {
                Toast.makeText(context, "Dokter berhasil ditambahkan.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Gagal menambahkan Dokter: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}


