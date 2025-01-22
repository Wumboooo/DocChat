package com.example.docchat.form

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class FirebaseHelper(private val context: Context, private val auth: FirebaseAuth) {

    fun startPhoneVerification(
        phoneNumber: String,
        name: String,
        gender: String,
        birthday: String,
        location: String,
        activity: Activity
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    auth.signInWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(context, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    val intent = Intent(context, OTPConfirmationActivity::class.java).apply {
                        putExtra("verificationId", verificationId)
                        putExtra("name", name)
                        putExtra("gender", gender)
                        putExtra("birthday", birthday)
                        putExtra("location", location)
                    }
                    context.startActivity(intent)
                }
            }).build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}
