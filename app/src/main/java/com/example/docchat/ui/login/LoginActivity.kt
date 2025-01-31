package com.example.docchat.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.docchat.R
import com.example.docchat.SplashScreenActivity.Companion.globalRole
import com.example.docchat.ui.MainActivity
import com.example.docchat.ui.form.ProfileFormActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var authManager: AuthManager
    private lateinit var userRepository: UserRepository
    private lateinit var loadingBar: ProgressBar

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val auth = FirebaseAuth.getInstance()
        val googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build())

        authManager = AuthManager(auth, googleSignInClient)
        userRepository = UserRepository(FirebaseFirestore.getInstance())
        loadingBar = findViewById(R.id.loadingBar)

        findViewById<SignInButton>(R.id.signInButton).setOnClickListener {
            loadingBar.visibility = View.VISIBLE
            authManager.signIn(this, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            loadingBar.visibility = View.VISIBLE
            authManager.handleSignInResult(data,
                onSuccess = { account ->
                    authManager.authenticateWithGoogle(account) { user ->
                        user?.email?.let { email ->
                            userRepository.checkUserRole(email) { role ->
                                globalRole = role
                                when (role) {
                                    "admin", "doctor" -> navigateTo(MainActivity::class.java)
                                    "user" -> userRepository.isProfileCompleted(email) { isCompleted ->
                                        if (isCompleted) {
                                            navigateTo(MainActivity::class.java)
                                        } else {
                                            navigateTo(ProfileFormActivity::class.java)
                                        }
                                    }
                                    else -> showLoginError()
                                }
                            }
                        }
                    }
                },
                onFailure = {
                    showLoginError()
                }
            )
        }
    }

    private fun navigateTo(activity: Class<*>) {
        loadingBar.visibility = View.GONE
        startActivity(Intent(this, activity))
        finish()
    }

    private fun showLoginError() {
        loadingBar.visibility = View.GONE
        Toast.makeText(this, "Google Sign-in failed", Toast.LENGTH_SHORT).show()
    }
}

