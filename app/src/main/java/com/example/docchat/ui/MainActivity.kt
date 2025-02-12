package com.example.docchat.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.docchat.R
import com.example.docchat.SplashScreenActivity.Companion.globalRole
import com.example.docchat.databinding.ActivityMainBinding
import com.example.docchat.ui.adminlist.AdminDoctorListActivity
import com.example.docchat.ui.form.ProfileFormActivity
import com.example.docchat.ui.login.LoginActivity
import com.example.docchat.ui.profile.ProfileRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isMasterAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.navigationBarColor = resources.getColor(R.color.darkgreen, theme)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        val navView: BottomNavigationView = binding.navView
        navView.setupWithNavController(navController)
        navView.itemIconTintList = null

        val chatId = intent.getStringExtra("chatId")
        if (chatId != null) {
            val bundle = Bundle()
            bundle.putString("chatId", chatId)

            navController.navigate(R.id.navigation_home, bundle)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val profileRepository = ProfileRepository()
        profileRepository.saveProfileImageToFirestore()
        checkAdminTier()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val listAdminAndDoctor = menu?.findItem(R.id.list_admin_and_doctor)
        val editProfile = menu?.findItem(R.id.action_edit_profile)
        listAdminAndDoctor?.isVisible = isMasterAdmin
        editProfile?.isVisible = globalRole == "user"

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sign_out -> {
                signOut()
                true
            }
            R.id.list_admin_and_doctor -> {
                val intent = Intent(this, AdminDoctorListActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_edit_profile -> {
                startActivity(Intent(this, ProfileFormActivity::class.java).apply {
                    putExtra("edit_mode", true)
                })
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkAdminTier() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val email = user.email ?: return

            FirebaseFirestore.getInstance().collection("admins").document(email)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val tier = document.getString("tier")
                        isMasterAdmin = (tier == "master")
                    } else {
                        isMasterAdmin = false
                    }
                    invalidateOptionsMenu() // Perbarui menu setelah mendapatkan data
                }
                .addOnFailureListener {
                    isMasterAdmin = false
                    invalidateOptionsMenu()
                }
        } else {
            isMasterAdmin = false
        }
    }

    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}

