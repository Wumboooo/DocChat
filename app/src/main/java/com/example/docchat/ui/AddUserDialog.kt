package com.example.docchat.ui

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
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore


class AddUserDialog(private val context: Context, private val firestore: FirebaseFirestore) {

    fun show() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Tambah Admin atau Dokter")

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_user, null)
        val emailEditText = view.findViewById<EditText>(R.id.emailEditText)
        val nameEditText = view.findViewById<EditText>(R.id.nameEditText)
        val specializationEditText = view.findViewById<EditText>(R.id.specializationEditText)
        val feeEditText = view.findViewById<EditText>(R.id.feeEditText)
        val experienceEditText = view.findViewById<EditText>(R.id.experienceEditText)
        val roleSpinner = view.findViewById<Spinner>(R.id.roleSpinner)

        specializationEditText.visibility = View.GONE
        feeEditText.visibility = View.GONE
        experienceEditText.visibility = View.GONE

        val roles = arrayOf("Admin", "Dokter")
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, roles)
        roleSpinner.adapter = adapter

        roleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedRole = roles[position]
                if (selectedRole == "Admin") {
                    specializationEditText.visibility = View.GONE
                    feeEditText.visibility = View.GONE
                    experienceEditText.visibility = View.GONE
                } else {
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
            val role = roleSpinner.selectedItem.toString()

            if (email.isEmpty() || name.isEmpty()) {
                Toast.makeText(context, "Email dan nama harus diisi.", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            validateEmail(email) { isUnique ->
                if (isUnique) {
                    if (role == "Dokter") {
                        val specialization = specializationEditText.text.toString().trim()
                        val fee = feeEditText.text.toString().trim()
                        val experience = experienceEditText.text.toString().trim()

                        if (specialization.isEmpty() || fee.isEmpty() || experience.isEmpty()) {
                            Toast.makeText(context, "Semua field dokter harus diisi.", Toast.LENGTH_SHORT).show()
                            return@validateEmail
                        }

                        addDoctorToDatabase(email, name, specialization, fee, experience)
                    } else {
                        addAdminToDatabase(email, name)
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
        val usersCollection = listOf("users", "admins", "doctors")
        var isUnique = true

        val tasks = usersCollection.map { collection ->
            firestore.collection(collection).document(email).get()
        }

        Tasks.whenAllComplete(tasks)
            .addOnSuccessListener {
                for (task in tasks) {
                    val result = (task as Task<DocumentSnapshot>).result
                    if (result.exists()) {
                        isUnique = false
                        break
                    }
                }
                callback(isUnique)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun addAdminToDatabase(email: String, name: String) {
        firestore.collection("admins").document(email)
            .set("name" to name)
            .addOnSuccessListener {
                Toast.makeText(context, "Admin berhasil ditambahkan.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Gagal menambahkan Admin: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addDoctorToDatabase(email: String, name: String, specialization: String, fee: String, experience: String) {
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
