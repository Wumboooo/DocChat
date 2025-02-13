package com.example.docchat.ui.login

import android.util.Log
import com.example.docchat.SplashScreenActivity.Companion.globalRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

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


    fun getNewFCMToken() {
        val firebaseMessaging = FirebaseMessaging.getInstance()

        firebaseMessaging.deleteToken() // Hapus token lama sebelum mendapatkan yang baru
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    firebaseMessaging.token.addOnSuccessListener { newToken ->
                        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: return@addOnSuccessListener
                        val collection = when(globalRole) {
                            "admin" -> "admins"
                            "doctor" -> "doctors"
                            else -> "users"
                        }
                        firestore.collection(collection)
                            .document(currentUserEmail)
                            .update("fcmToken", newToken)
                            .addOnSuccessListener {
                                Log.d("FCM", "Token FCM diperbarui untuk $currentUserEmail: $newToken")
                            }
                            .addOnFailureListener { e ->
                                Log.e("FCM", "Gagal memperbarui token FCM", e)
                            }
                    }
                } else {
                    Log.e("FCM", "Gagal menghapus token lama", task.exception)
                }
            }
    }

}