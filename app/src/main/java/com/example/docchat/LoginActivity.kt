package com.example.docchat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
            startActivity(Intent(this, ProfileFormActivity::class.java))
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
                        checkUserInFirestore(it)
                        Toast.makeText(this, "Welcome ${user?.displayName}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    loadingBar.visibility = View.GONE
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkUserInFirestore(user: FirebaseUser) {
        val userRef = firestore.collection("users").document(user.uid)
        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val userData = document.data
                // Check if the required fields are filled
                val isProfileComplete = userData?.get("phoneNumber") != null &&
                        userData["gender"] != null &&
                        userData["birthday"] != null &&
                        userData["location"] != null

                if (isProfileComplete) {
                    navigateToHome()
                } else {
                    navigateToProfileForm()
                }
            } else {
                navigateToProfileForm()
            }
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