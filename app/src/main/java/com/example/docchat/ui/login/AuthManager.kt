package com.example.docchat.ui.login

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class AuthManager(private val auth: FirebaseAuth, private val googleSignInClient: GoogleSignInClient) {
    fun signIn(activity: Activity, requestCode: Int) {
        val signInIntent = googleSignInClient.signInIntent
        activity.startActivityForResult(signInIntent, requestCode)
    }

    fun handleSignInResult(data: Intent?, onSuccess: (GoogleSignInAccount) -> Unit, onFailure: (Exception) -> Unit) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                onSuccess(account)
            }
        } catch (e: ApiException) {
            onFailure(e)
        }
    }

    fun authenticateWithGoogle(account: GoogleSignInAccount, onComplete: (FirebaseUser?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onComplete(auth.currentUser)
            } else {
                onComplete(null)
            }
        }
    }
}