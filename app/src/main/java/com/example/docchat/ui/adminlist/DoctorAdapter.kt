package com.example.docchat.ui.adminlist

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.docchat.R
import com.example.docchat.ui.Doctor
import com.google.firebase.firestore.FirebaseFirestore

class DoctorAdapter(
    private val doctorList: MutableList<Doctor>,
    private val firestore: FirebaseFirestore,
    private val context: Context,
    private val currentEmail: String?,
    private val onDelete: (Doctor) -> Unit
) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {

    class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDoctorName: TextView = itemView.findViewById(R.id.tvDoctorName)
        val tvSpecialization: TextView = itemView.findViewById(R.id.tvSpecialization)
        val tvExperience: TextView = itemView.findViewById(R.id.tvExperience)
        val tvFee: TextView = itemView.findViewById(R.id.tvFee)
        val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_doctor, parent, false)
        return DoctorViewHolder(view)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        val doctor = doctorList[position]
        holder.tvDoctorName.text = doctor.name
        holder.tvSpecialization.text = "Spesialisasi: ${doctor.specialization}"
        holder.tvExperience.text = "Pengalaman: ${doctor.experience} tahun"
        holder.tvFee.text = "Biaya: Rp ${doctor.fee}"
        holder.tvEmail.text = "Email: ${doctor.email}"

        if (doctor.email == currentEmail) {
            holder.btnDelete.visibility = View.GONE
        } else {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Konfirmasi Hapus")
                    .setMessage("Apakah Anda yakin ingin menghapus ${doctor.name}?")
                    .setPositiveButton("Hapus") { _, _ -> onDelete(doctor) }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }

        holder.btnEdit.setOnClickListener {
            showEditUserDialog(doctor)
        }
    }

    override fun getItemCount(): Int = doctorList.size

    private fun showEditUserDialog(doctor: Doctor) {
        val editUserDialog = EditUserDialog(context, firestore, doctor)
        editUserDialog.show()
    }
}

