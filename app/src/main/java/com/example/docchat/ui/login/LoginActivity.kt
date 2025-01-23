package com.example.docchat.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.docchat.R
import com.example.docchat.SplashScreenActivity
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

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        loadingBar = findViewById(R.id.loadingBar)

        // Google Sign-In setup
        googleSignInClient = GoogleSignIn.getClient(this, getGoogleSignInOptions())
        findViewById<SignInButton>(R.id.signInButton).setOnClickListener { signIn() }
    }

    private fun getGoogleSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }

    private fun signIn() {
        loadingBar.visibility = View.VISIBLE
        startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.let { firebaseAuthWithGoogle(it) }
            } catch (e: ApiException) {
                loadingBar.visibility = View.GONE
                Log.w("LoginActivity", "Google sign-in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.let {
                        val email = it.email ?: "No email"
                        saveEmailToFirestore(email, it.uid)
                        navigateTo(SplashScreenActivity::class.java)
                        Toast.makeText(this, "Welcome ${it.displayName}", Toast.LENGTH_SHORT).show()
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

    private fun navigateTo(destination: Class<*>) {
        loadingBar.visibility = View.GONE
        startActivity(Intent(this, destination))
        finish()
    }
}
