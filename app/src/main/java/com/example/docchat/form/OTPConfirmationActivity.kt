package com.example.docchat.form

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.docchat.MainActivity
import com.example.docchat.R
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class OTPConfirmationActivity : AppCompatActivity() {

    private lateinit var otpEditText: EditText
    private lateinit var confirmButton: Button
    private lateinit var resendButton: Button
    private lateinit var skipOtpButton: Button
    private var verificationId: String? = null
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_confirmation)

        auth = FirebaseAuth.getInstance()
        otpEditText = findViewById(R.id.otpEditText)
        confirmButton = findViewById(R.id.confirmButton)
        resendButton = findViewById(R.id.resendButton)
        skipOtpButton = findViewById(R.id.skipOtpButton)

        verificationId = intent.getStringExtra("verificationId")

        confirmButton.setOnClickListener { verifyOTP() }
        resendButton.setOnClickListener { resendOTP() }
        skipOtpButton.setOnClickListener { navigateToMain() }

    }

    private fun verifyOTP() {
        val otpCode = otpEditText.text.toString().trim()
        if (otpCode.isEmpty() || otpCode.length != 6) {
            Toast.makeText(this, "Please enter a valid OTP", Toast.LENGTH_SHORT).show()
            return
        }

        verificationId?.let {
            val credential = PhoneAuthProvider.getCredential(it, otpCode)
            signInWithCredential(credential)
        } ?: run {
            Toast.makeText(this, "Verification ID is missing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Phone number verified successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Verification failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun resendOTP() {
        val phoneNumber = intent.getStringExtra("phoneNumber") ?: return Toast.makeText(
            this, "Phone number missing", Toast.LENGTH_SHORT).show()
        sendVerificationCode(phoneNumber)
    }

    private fun sendVerificationCode(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Toast.makeText(this@OTPConfirmationActivity, "Verification completed automatically", Toast.LENGTH_SHORT).show()
                    signInWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@OTPConfirmationActivity, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    this@OTPConfirmationActivity.verificationId = verificationId
                    Toast.makeText(this@OTPConfirmationActivity, "OTP sent successfully", Toast.LENGTH_SHORT).show()
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
