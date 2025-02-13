package com.example.docchat.ui.chat

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.docchat.ui.Messages
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatRepository(private val firestore: FirebaseFirestore) {

    fun sendMessage(chatId: String, senderEmail: String, text: String, context: Context, onComplete: () -> Unit) {
        if (text.isBlank()) return

        isChatClosed(chatId) { isClosed ->
            if (isClosed) {
                Toast.makeText(context, "Status: Closed, hubungi admin untuk tersambung kembali.", Toast.LENGTH_SHORT).show()
            } else {
                val message = mapOf(
                    "senderEmail" to senderEmail,
                    "text" to text,
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "sent",
                    "isRead" to false
                )

                val chatRef = firestore.collection("chats").document(chatId)

                chatRef.collection("messages").add(message).addOnSuccessListener { messageDoc ->
                    chatRef.update(
                        "lastMessage", text,
                        "lastUpdated", System.currentTimeMillis(),
                        "lastSenderEmail", senderEmail.lowercase()
                    )
                    onComplete()
                }
            }
        }
    }

    fun markMessagesAsRead(chatId: String, currentUserEmail: String) {
        val chatRef = firestore.collection("chats").document(chatId)
        chatRef.collection("messages")
            .whereEqualTo("isRead", false) // Hanya pesan yang belum dibaca
            .whereNotEqualTo("senderEmail", currentUserEmail) // Hanya pesan dari orang lain
            .get()
            .addOnSuccessListener { documents ->
                val batch = firestore.batch()
                for (document in documents) {
                    batch.update(document.reference, "isRead", true, "status", "delivered")
                }
                batch.commit()
                    .addOnSuccessListener { Log.d("ChatRepository", "Pesan ditandai sebagai dibaca.") }
                    .addOnFailureListener { e -> Log.e("ChatRepository", "Gagal menandai pesan sebagai dibaca", e) }
            }
    }

    fun loadMessages(chatId: String, onMessagesLoaded: (List<Messages>) -> Unit) {
        val chatRef = firestore.collection("chats").document(chatId)

        chatRef.collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { it.toObject(Messages::class.java) }
                    onMessagesLoaded(messages)
                }
            }
    }

    fun isChatClosed(chatId: String, callback: (Boolean) -> Unit) {
        firestore.collection("chats").document(chatId).get()
            .addOnSuccessListener { doc ->
                val status = doc.getString("status") ?: "active" // Default ke "active"
                callback(status == "closed")
            }
            .addOnFailureListener {
                callback(false) // Jika gagal, anggap chat tetap aktif
            }
    }
}
