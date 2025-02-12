package com.example.docchat.ui.profile

import android.util.Log
import com.example.docchat.ui.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun saveProfileImageToFirestore() {
        val user = auth.currentUser ?: return
        val photoUrl = user.photoUrl?.toString() ?: return
        val email = user.email ?: return

        val collections = listOf("users", "admins", "doctors")

        checkAndUpdateProfileImage(collections, email, photoUrl, 0)
    }

    private fun checkAndUpdateProfileImage(collections: List<String>, email: String, photoUrl: String, index: Int) {
        if (index >= collections.size) {
            Log.e("ProfileRepository", "Email tidak ditemukan di koleksi mana pun")
            return
        }

        val collection = collections[index]
        val userDocRef = db.collection(collection).document(email)

        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // Jika dokumen ditemukan, update profileImage
                userDocRef.update("profileImage", photoUrl)
                    .addOnSuccessListener {
                        Log.d("ProfileRepository", "Foto profil diperbarui di Firestore pada koleksi: $collection")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfileRepository", "Gagal memperbarui foto profil: ${e.message}")
                    }
            } else {
                checkAndUpdateProfileImage(collections, email, photoUrl, index + 1)
            }
        }.addOnFailureListener { e ->
            Log.e("ProfileRepository", "Gagal mengambil data dari koleksi $collection: ${e.message}")
        }
    }

    fun getUserProfile(onComplete: (UserProfile?) -> Unit) {
        val user = auth.currentUser ?: return

        val collections = listOf("users", "admins", "doctors")
        fetchFromCollections(user.email!!, collections, onComplete)
    }

    private fun fetchFromCollections(email: String, collections: List<String>, onComplete: (UserProfile?) -> Unit, index: Int = 0) {
        if (index >= collections.size) {
            onComplete(null) // Jika tidak ditemukan di semua koleksi
            return
        }

        db.collection(collections[index]).document(email)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val profile = document.toObject(UserProfile::class.java)
                    onComplete(profile)
                } else {
                    fetchFromCollections(email, collections, onComplete, index + 1) // Coba koleksi berikutnya
                }
            }
            .addOnFailureListener {
                Log.e("ProfileRepository", "Failed to fetch profile from ${collections[index]}", it)
                fetchFromCollections(email, collections, onComplete, index + 1) // Coba koleksi berikutnya
            }
    }

}
