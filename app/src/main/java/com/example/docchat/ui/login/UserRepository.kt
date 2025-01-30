package com.example.docchat.ui.login

import com.google.firebase.firestore.FirebaseFirestore

class UserRepository(private val firestore: FirebaseFirestore) {
    fun checkUserRole(email: String, callback: (String) -> Unit) {
        firestore.collection("admins").document(email).get()
            .addOnSuccessListener { adminSnapshot ->
                if (adminSnapshot.exists()) {
                    callback("admin")
                } else {
                    firestore.collection("doctors").document(email).get()
                        .addOnSuccessListener { doctorSnapshot ->
                            if (doctorSnapshot.exists()) {
                                callback("doctor")
                            } else {
                                callback("user")
                            }
                        }
                        .addOnFailureListener { callback("user") }
                }
            }
            .addOnFailureListener { callback("user") }
    }

    fun isProfileCompleted(email: String, callback: (Boolean) -> Unit) {
        firestore.collection("users").document(email).get()
            .addOnSuccessListener { document ->
                val isCompleted = document.getBoolean("isProfileCompleted") ?: false
                callback(isCompleted)
            }
            .addOnFailureListener {
                callback(false)  // Jika gagal, anggap profil belum selesai
            }
    }

}