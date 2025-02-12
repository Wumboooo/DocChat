package com.example.docchat.ui.form

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.docchat.ui.MainActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.concurrent.TimeUnit

class FirebaseHelper(private val context: Context, private val auth: FirebaseAuth) {

    fun saveUserProfileToFirestore(
        email: String,
        name: String,
        gender: String,
        phoneNumber: String,
        birthday: String,
        location: String,
        isProfileCompleted: Boolean,
        onComplete: () -> Unit // Callback){}
    ) {
        val db = FirebaseFirestore.getInstance()
        val userData = mapOf(
            "name" to name,
            "gender" to gender,
            "phone" to phoneNumber,
            "birthday" to birthday,
            "location" to location,
            "isProfileCompleted" to isProfileCompleted
        )

        db.collection("users").document(email)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(context, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                val intent = Intent(context, MainActivity::class.java) // lewati phone verification
                context.startActivity(intent)
                onComplete() // Panggil callback setelah selesai
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to save profile: ${e.message}", Toast.LENGTH_SHORT).show()
                onComplete() // Tetap panggil callback untuk sembunyikan loading
            }
    }

    interface PhoneVerificationCallback {
        fun onVerificationComplete()
        fun onVerificationFailed(error: String)
    }

    fun startPhoneVerification(phoneNumber: String, activity: AppCompatActivity, callback: PhoneVerificationCallback) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                callback.onVerificationComplete()
                            } else {
                                callback.onVerificationFailed(task.exception?.message ?: "Unknown error")
                            }
                        }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    callback.onVerificationFailed(e.message ?: "Unknown error")
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    val intent = Intent(context, OTPConfirmationActivity::class.java).apply {
                        putExtra("verificationId", verificationId)
                        putExtra("phoneNumber", phoneNumber)
                    }
                    context.startActivity(intent)
                }
            }).build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }


}

