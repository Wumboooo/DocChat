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
import com.google.firebase.auth.FirebaseUser
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if the user is already signed in
        if (FirebaseAuth.getInstance().currentUser != null) {
            // User is signed in, navigate directly to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish() // Close LoginActivity
            return
        }

        setContentView(R.layout.activity_login)
        loadingBar = findViewById(R.id.loadingBar)

        // Rest of your login setup code
        auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        findViewById<SignInButton>(R.id.signInButton).setOnClickListener {
            signIn()
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
                Log.w("LoginActivity", "Google sign in failed", e)
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
                        val email = it.email ?: "No email"
                        saveEmailToFirestore(email, it.uid)
                        checkUserInFirestore(it)
                        Toast.makeText(this, "Welcome ${user?.displayName}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    loadingBar.visibility = View.GONE
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveEmailToFirestore(email: String, userId: String) {
        val db = FirebaseFirestore.getInstance()
        val userData = mapOf("email" to email)

        db.collection("users").document(userId)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("LoginActivity", "Email saved to Firestore successfully")
            }
            .addOnFailureListener { e ->
                Log.e("LoginActivity", "Error saving email to Firestore", e)
            }
    }

    private fun checkUserInFirestore(user: FirebaseUser) {
        val userRef = firestore.collection("users").document(user.uid)
        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val isProfileComplete = document.getBoolean("isProfileComplete") ?: false
                if (isProfileComplete) {
                    navigateToHome()
                } else {
                    navigateToProfileForm()
                }
            } else {
                // Buat entri baru dengan isProfileComplete = false
                val userData = mapOf(
                    "email" to user.email,
                    "isProfileComplete" to false
                )
                userRef.set(userData, SetOptions.merge()).addOnSuccessListener {
                    navigateToProfileForm()
                }
            }
        }.addOnFailureListener { e ->
            Log.e("LoginActivity", "Failed to fetch user data", e)
            Toast.makeText(this, "Failed to verify user data", Toast.LENGTH_SHORT).show()
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
        // Ensure all necessary data is saved before navigating
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
