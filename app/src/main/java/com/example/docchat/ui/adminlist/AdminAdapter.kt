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
import com.example.docchat.ui.Admin
import com.google.firebase.firestore.FirebaseFirestore

class AdminAdapter(
    private val adminList: MutableList<Admin>,
    private val firestore: FirebaseFirestore,
    private val context: Context,
    private val currentEmail: String?,
    private val onDelete: (Admin) -> Unit
) : RecyclerView.Adapter<AdminAdapter.AdminViewHolder>() {

    class AdminViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAdminName: TextView = itemView.findViewById(R.id.tvAdminName)
        val tvAdminEmail: TextView = itemView.findViewById(R.id.tvAdminEmail)
        val tvAdminTier: TextView = itemView.findViewById(R.id.tvAdminTier)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin, parent, false)
        return AdminViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminViewHolder, position: Int) {
        val admin = adminList[position]
        holder.tvAdminName.text = admin.name
        holder.tvAdminEmail.text = admin.email
        holder.tvAdminTier.text = admin.tier

        if (admin.email == currentEmail) {
            holder.btnDelete.visibility = View.GONE
        } else {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Konfirmasi Hapus")
                    .setMessage("Apakah Anda yakin ingin menghapus ${admin.name}?")
                    .setPositiveButton("Hapus") { _, _ -> onDelete(admin) }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }

        holder.btnEdit.setOnClickListener {
            showEditUserDialog(admin)
        }
    }

    override fun getItemCount(): Int = adminList.size

    private fun showEditUserDialog(admin: Admin) {
        val editUserDialog = EditUserDialog(context, firestore, admin)
        editUserDialog.show()
    }
}


