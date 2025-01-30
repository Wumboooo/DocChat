package com.example.docchat.ui.profile

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.docchat.ui.ChatSummary
import com.example.docchat.R
import com.example.docchat.ui.login.LoginActivity.Companion.globalRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var summaryAdapter: SummaryAdapter
    private val summaryList = mutableListOf<ChatSummary>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        summaryAdapter = SummaryAdapter(summaryList) { summary ->
            handleSummaryClick(summary)
        }
        recyclerView.adapter = summaryAdapter

        loadSummaries()

        // Set up touch listener to dismiss keyboard
        view.setOnTouchListener { _, _ ->
            dismissKeyboard()
            false
        }

        return view
    }

    private fun loadSummaries() {
        val currentUserEmail = auth.currentUser?.email ?: return
        val query = if (globalRole == "doctor" || globalRole == "admin") {
            firestore.collection("chats")
        } else {
            firestore.collection("chats").whereArrayContains("participants", currentUserEmail)
        }

        query.whereEqualTo("status", "closed").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("ProfileFragment", "Error fetching summaries: ${error.message}")
                return@addSnapshotListener
            }

            val updatedSummaries = snapshot?.documents?.mapNotNull { doc ->
                doc.toChatSummary()
            } ?: emptyList()

            summaryList.clear()
            summaryList.addAll(updatedSummaries)
            summaryAdapter.notifyDataSetChanged()
        }
    }

    private fun DocumentSnapshot.toChatSummary(): ChatSummary? {
        val summaryData = this.get("summary") as? Map<String, Any> ?: return null
        return ChatSummary(
            summaryId = id,
            patientEmail = summaryData["patientEmail"] as? String ?: "",
            doctorName = summaryData["doctorName"] as? String ?: "",
            date = summaryData["date"] as? Long ?: 0,
            disease = summaryData["disease"] as? String ?: "",
            medicine = summaryData["medicine"] as? String ?: ""
        )
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

        // Ensure Enter adds a new line
        diseaseEditText.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                diseaseEditText.append("\n")
                true
            } else {
                false
            }
        }

        medicineEditText.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                medicineEditText.append("\n")
                true
            } else {
                false
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Summary")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val updatedDisease = diseaseEditText.text.toString()
                val updatedMedicine = medicineEditText.text.toString()

                updateSummary(summary.summaryId, updatedDisease, updatedMedicine)
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

        firestore.collection("chats").document(summaryId)
            .update("summary", summaryData)
            .addOnSuccessListener {
                Toast.makeText(context, "Ringkasan berhasil diperbarui.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal memperbarui ringkasan.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun dismissKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocus = requireActivity().currentFocus
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
        }
    }
}

