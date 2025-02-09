package com.example.docchat.ui.chat

import android.content.Context
import android.widget.Toast
import com.example.docchat.ui.Messages
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatRepository(private val firestore: FirebaseFirestore) {

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

    fun sendMessage(chatId: String, auth: FirebaseAuth, text: String, context: Context, onComplete: () -> Unit) {
        if (text.isBlank()) return

        isChatClosed(chatId) { isClosed ->
            if (isClosed) {
                Toast.makeText(context, "status: Closed, hubungi admin untuk tersambung kembali.", Toast.LENGTH_SHORT).show()
            } else {
                val message = mapOf(
                    "senderEmail" to auth.currentUser!!.email,
                    "text" to text,
                    "timestamp" to System.currentTimeMillis()
                )

                val chatRef = firestore.collection("chats").document(chatId)
                chatRef.collection("messages").add(message).addOnSuccessListener {
                    chatRef.update(
                        "lastMessage", text,
                        "lastUpdated", System.currentTimeMillis()
                    )
                    onComplete()
                }
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
