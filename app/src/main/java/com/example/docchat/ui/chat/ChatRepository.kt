package com.example.docchat.ui.chat

import com.example.docchat.ui.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatRepository(private val firestore: FirebaseFirestore) {

    fun loadMessages(chatId: String, onMessagesLoaded: (List<Message>) -> Unit) {
        val chatRef = firestore.collection("chats").document(chatId)
        chatRef.collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { it.toObject(Message::class.java) }
                    onMessagesLoaded(messages)
                }
            }
    }

    fun sendMessage(chatId: String, auth: FirebaseAuth, text: String, onComplete: () -> Unit) {
        if (text.isBlank()) return

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
