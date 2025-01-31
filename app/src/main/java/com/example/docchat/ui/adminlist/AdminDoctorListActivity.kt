package com.example.docchat.ui.adminlist

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.docchat.R
import com.example.docchat.ui.Admin
import com.example.docchat.ui.Doctor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDoctorListActivity : AppCompatActivity() {

    private lateinit var adminRecyclerView: RecyclerView
    private lateinit var doctorRecyclerView: RecyclerView

    private lateinit var adminAdapter: AdminAdapter
    private lateinit var doctorAdapter: DoctorAdapter

    private val adminList = mutableListOf<Admin>()
    private val doctorList = mutableListOf<Doctor>()
    private val db = FirebaseFirestore.getInstance()
    private var currentEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_doctor_list)

        currentEmail = FirebaseAuth.getInstance().currentUser?.email

        adminRecyclerView = findViewById(R.id.rvAdmins)
        doctorRecyclerView = findViewById(R.id.rvDoctors)

        adminRecyclerView.layoutManager = LinearLayoutManager(this)
        doctorRecyclerView.layoutManager = LinearLayoutManager(this)

        adminAdapter = AdminAdapter(adminList, db, this, currentEmail) { admin ->
            deleteAdmin(admin)
        }

        doctorAdapter = DoctorAdapter(doctorList, db, this, currentEmail) { doctor ->
            deleteDoctor(doctor)
        }

        adminRecyclerView.adapter = adminAdapter
        doctorRecyclerView.adapter = doctorAdapter

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        fetchAdmins()
        fetchDoctors()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_user, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_user -> {
                showAddUserDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun fetchAdmins() {
        db.collection("admins")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("Firestore", "Listen failed.", e)
                    return@addSnapshotListener
                }

                adminList.clear()
                snapshots?.forEach { document ->
                    val admin = document.toObject(Admin::class.java)
                    admin.email = document.id
                    adminList.add(admin)
                }
                adminAdapter.notifyDataSetChanged()
            }
    }

    private fun fetchDoctors() {
        db.collection("doctors")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("Firestore", "Listen failed.", e)
                    return@addSnapshotListener
                }

                doctorList.clear()
                snapshots?.forEach { document ->
                    val doctor = document.toObject(Doctor::class.java)
                    doctor.email = document.id
                    doctorList.add(doctor)
                }
                doctorAdapter.notifyDataSetChanged()
            }
    }

    private fun deleteAdmin(admin: Admin) {
        db.collection("admins").document(admin.email)
            .delete()
            .addOnSuccessListener {
                adminList.remove(admin)
                adminAdapter.notifyDataSetChanged()
                fetchAdmins() // Refresh list setelah hapus
                Toast.makeText(this, "Admin berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus admin: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteDoctor(doctor: Doctor) {
        db.collection("doctors").document(doctor.email)
            .delete()
            .addOnSuccessListener {
                doctorList.remove(doctor)
                doctorAdapter.notifyDataSetChanged()
                fetchDoctors() // Refresh list setelah hapus
                Toast.makeText(this, "Dokter berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus dokter: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddUserDialog() {
        val addUserDialog = AddUserDialog(this, db)
        addUserDialog.show()
    }

}
