package com.example.docchat.ui.chat

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.docchat.ui.Messages
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatRepository(private val firestore: FirebaseFirestore) {

    fun sendMessage(chatId: String, auth: FirebaseAuth, text: String, context: Context, onComplete: () -> Unit) {
        if (text.isBlank()) return

        isChatClosed(chatId) { isClosed ->
            if (isClosed) {
                Toast.makeText(context, "Status: Closed, hubungi admin untuk tersambung kembali.", Toast.LENGTH_SHORT).show()
            } else {
                val message = mapOf(
                    "senderEmail" to auth.currentUser!!.email,
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
                        "lastSenderEmail", auth.currentUser!!.email?.lowercase() ?: "email not found"
                    )

                    chatRef.get().addOnSuccessListener { chatSnapshot ->
                        val participantEmails = chatSnapshot.get("participants") as? List<String> ?: listOf()
                        val recipientEmail = participantEmails.firstOrNull { it != auth.currentUser!!.email }

                        isUserInChatActivity(recipientEmail, chatId) { isActive ->
                            if (isActive) {
                                messageDoc.update("status", "delivered")
                            }
                        }
                    }

                    onComplete()
                }
            }
        }
    }

    fun isUserInChatActivity(recipientEmail: String?, chatId: String, callback: (Boolean) -> Unit) {
        val userStatusRef = firestore.collection("users").document(recipientEmail?: "")

        userStatusRef.get().addOnSuccessListener { document ->
            val lastActive = document.getLong("lastActive") ?: 0L
            val currentTime = System.currentTimeMillis()

            callback(currentTime - lastActive < 10_000)
        }.addOnFailureListener {
            callback(false)
        }
    }

    fun markMessagesAsRead(chatId: String) {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: return

        val chatRef = firestore.collection("chats").document(chatId).collection("messages")

        chatRef.whereEqualTo("isRead", false)
            .whereNotEqualTo("senderEmail", currentUserEmail)  // Hanya pesan yang dikirim oleh lawan chat
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) return@addOnSuccessListener  // Jika tidak ada pesan yang perlu diperbarui, hentikan

                val batch = firestore.batch()
                for (document in documents) {
                    val messageRef = document.reference
                    batch.update(messageRef, "isRead", true)
                }
                batch.commit()
            }
            .addOnFailureListener { e ->
                Log.e("ChatRepository", "Gagal memperbarui status pesan", e)
            }
    }

    fun loadMessages(chatId: String, onMessagesLoaded: (List<Messages>) -> Unit) {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val chatRef = firestore.collection("chats").document(chatId)

        chatRef.collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { it.toObject(Messages::class.java) }
                    onMessagesLoaded(messages)

                    val batch = firestore.batch()
                    for (document in snapshot.documents) {
                        val isRead = document.getBoolean("isRead") ?: false
                        val senderEmail = document.getString("senderEmail") ?: ""

                        // If the message is unread and sent by the other user, mark it as read
                        if (!isRead && senderEmail != currentUserEmail) {
                            batch.update(document.reference, "isRead", true)
                        }
                    }
                    batch.commit()
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
