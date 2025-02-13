package com.example.docchat.ui.summary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.docchat.R
import com.example.docchat.SplashScreenActivity.Companion.globalRole
import com.example.docchat.ui.ChatSummary
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SummaryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var summaryAdapter: SummaryAdapter
    private lateinit var viewModel: SummaryViewModel
    private lateinit var summariesTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_summary, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        summariesTextView = view.findViewById(R.id.summariesTextView)

        setupRecyclerView()
        setupViewModel()

        return view
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        summaryAdapter = SummaryAdapter(emptyList()) { handleSummaryClick(it) }
        recyclerView.adapter = summaryAdapter
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[SummaryViewModel::class.java]

        viewModel.summaries.observe(viewLifecycleOwner) { summaries ->
            summaryAdapter = SummaryAdapter(summaries) { handleSummaryClick(it) }
            recyclerView.adapter = summaryAdapter
            summariesTextView.visibility = if (summaries.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loadSummaries(globalRole)
    }

    private fun handleSummaryClick(summary: ChatSummary) {
        if (globalRole == "doctor" || globalRole == "admin") {
            showEditSummaryDialog(summary)
        } else {
            Toast.makeText(context, "Hanya dokter atau admin yang dapat mengubah summary.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditSummaryDialog(summary: ChatSummary) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_summary, null)
        val diseaseEditText = dialogView.findViewById<EditText>(R.id.diseaseEditText)
        val medicineEditText = dialogView.findViewById<EditText>(R.id.medicineEditText)
        diseaseEditText.setText(summary.disease)
        medicineEditText.setText(summary.medicine)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Summary")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                updateSummary(summary.summaryId, diseaseEditText.text.toString(), medicineEditText.text.toString())
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateSummary(summaryId: String?, disease: String, medicine: String) {
        if (summaryId == null) return
        val summaryData = mapOf(
            "disease" to disease,
            "medicine" to medicine
        )

        FirebaseFirestore.getInstance().collection("chats").document(summaryId)
            .set(summaryData, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(context, "Ringkasan berhasil diperbarui.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal memperbarui ringkasan.", Toast.LENGTH_SHORT).show()
            }
    }
}
