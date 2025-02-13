package com.example.docchat.ui.summary

import android.util.Log
import com.example.docchat.ui.ChatSummary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class SummaryRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun fetchSummaries(globalRole: String, onResult: (List<ChatSummary>) -> Unit) {
        val currentUserEmail = auth.currentUser?.email ?: return
        val query = if (globalRole == "doctor" || globalRole == "admin") {
            firestore.collection("chats")
        } else {
            firestore.collection("chats").whereArrayContains("participants", currentUserEmail)
        }

        query.whereEqualTo("status", "closed").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("SummaryRepository", "Error fetching summaries: ${error.message}")
                return@addSnapshotListener
            }

            val summaries = snapshot?.documents?.mapNotNull { it.toChatSummary() } ?: emptyList()
            onResult(summaries)
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
}
