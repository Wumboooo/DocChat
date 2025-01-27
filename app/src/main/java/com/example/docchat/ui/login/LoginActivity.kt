package com.example.docchat.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.docchat.MainActivity
import com.example.docchat.R
import com.example.docchat.ui.form.ProfileFormActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var loadingBar: ProgressBar
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val RC_SIGN_IN = 9001
        var globalRole = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        loadingBar = findViewById(R.id.loadingBar)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        findViewById<SignInButton>(R.id.signInButton).setOnClickListener {
            signIn()
        }

        // Check if user is already logged in
        auth.currentUser?.let { currentUser ->
            checkUserRoleAndNavigate(currentUser.email.orEmpty())
        }
    }

    private fun signIn() {
        loadingBar.visibility = View.VISIBLE
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                loadingBar.visibility = View.GONE
                Log.e("LoginActivity", "Google sign-in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val email = it.email.orEmpty()
                        createUserDocumentIfNeeded(email) {
                            checkUserRoleAndNavigate(email)
                        }
                    }
                } else {
                    loadingBar.visibility = View.GONE
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun createUserDocumentIfNeeded(email: String, callback: () -> Unit) {
        val userRef = firestore.collection("users").document(email)
        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val userData = mapOf("isProfileCompleted" to false)
                userRef.set(userData, SetOptions.merge())
                    .addOnSuccessListener { callback() }
                    .addOnFailureListener {
                        Log.e("LoginActivity", "Failed to create user document")
                        callback()
                    }
            } else {
                callback()
            }
        }.addOnFailureListener {
            Log.e("LoginActivity", "Failed to check user document")
            callback()
        }
    }

    private fun checkUserRoleAndNavigate(email: String) {
        checkUserRole(email) { role ->
            globalRole = role
            when (role) {
                "admin", "doctor" -> navigateToHome()
                "user" -> checkUserProfileStatus(email)
                else -> {
                    loadingBar.visibility = View.GONE
                    Toast.makeText(this, "Unknown role.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkUserRole(email: String, callback: (String) -> Unit) {
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

    private fun checkUserProfileStatus(email: String) {
        val userRef = firestore.collection("users").document(email)
        userRef.get().addOnSuccessListener { document ->
            val isProfileCompleted = document.getBoolean("isProfileCompleted") ?: false
            if (isProfileCompleted) {
                navigateToHome()
            } else {
                navigateToProfileForm()
            }
        }.addOnFailureListener {
            Log.e("LoginActivity", "Failed to fetch user profile status")
            navigateToProfileForm()
        }
    }

    private fun navigateToProfileForm() {
        loadingBar.visibility = View.GONE
        val intent = Intent(this, ProfileFormActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToHome() {
        loadingBar.visibility = View.GONE
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}