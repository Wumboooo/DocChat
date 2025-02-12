package com.example.docchat.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.docchat.R
import com.example.docchat.SplashScreenActivity.Companion.globalRole
import com.example.docchat.ui.ChatSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SummaryAdapter(
    private val summaries: List<ChatSummary>,
    private val onEditClick: (ChatSummary) -> Unit
) : RecyclerView.Adapter<SummaryAdapter.SummaryViewHolder>() {

    inner class SummaryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTextView: TextView = view.findViewById(R.id.dateTextView)
        val doctorTextView: TextView = view.findViewById(R.id.doctorTextView)
        val diseaseTextView: TextView = view.findViewById(R.id.diseaseTextView)
        val medicineTextView: TextView = view.findViewById(R.id.medicineTextView)
        val editButton: Button = view.findViewById(R.id.editButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_summary, parent, false)
        return SummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        val summary = summaries[position]
        holder.dateTextView.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(summary.date))
        holder.doctorTextView.text = "Dokter: ${summary.doctorName}"
        holder.diseaseTextView.text = "Penyakit: ${summary.disease}"
        holder.medicineTextView.text = "Resep Obat: ${summary.medicine}"
        holder.medicineTextView.visibility = if (globalRole == "user") View.GONE else View.VISIBLE
        holder.editButton.visibility = if (globalRole == "doctor" || globalRole == "admin") View.VISIBLE else View.GONE
        holder.editButton.setOnClickListener { onEditClick(summary) }
    }

    override fun getItemCount() = summaries.size
}
