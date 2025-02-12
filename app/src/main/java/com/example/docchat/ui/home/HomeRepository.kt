package com.example.docchat.ui.home

import android.util.Log
import com.example.docchat.ui.Chat
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import java.util.concurrent.atomic.AtomicInteger

class HomeRepository(private val firestore: FirebaseFirestore) {

    fun getUnreadMessagesCount(userEmail: String, callback: (Int) -> Unit) {
        firestore.collection("chats")
            .whereArrayContains("participants", userEmail)
            .get()
            .addOnSuccessListener { chats ->
                val unreadCount = AtomicInteger(0)
                val tasks = mutableListOf<Task<QuerySnapshot>>()

                for (chat in chats.documents) {
                    val task = firestore.collection("chats")
                        .document(chat.id)
                        .collection("messages")
                        .whereEqualTo("isRead", false)
                        .whereNotEqualTo("senderEmail", userEmail)
                        .get()
                        .addOnSuccessListener { messages ->
                            unreadCount.addAndGet(messages.size())
                        }
                    tasks.add(task)
                }

                Tasks.whenAllComplete(tasks).addOnCompleteListener {
                    callback(unreadCount.get())
                }
            }
    }

    fun fetchChats(currentEmail: String, onComplete: (List<Chat>) -> Unit) {
        firestore.collection("chats")
            .whereArrayContains("participants", currentEmail)
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error fetching chats: ${error.message}")
                    onComplete(emptyList())
                    return@addSnapshotListener
                }

                val chats = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Chat::class.java)?.apply { chatId = doc.id }
                } ?: emptyList()

                onComplete(chats)
            }
    }

    fun fetchParticipantName(email: String, callback: (String) -> Unit) {
        firestore.collection("admins").document(email).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    callback(doc.getString("name") ?: email)
                } else {
                    fetchFromAdminOrDoctor(email, callback)
                }
            }
            .addOnFailureListener {
                fetchFromAdminOrDoctor(email, callback)
            }
    }

    private fun fetchFromAdminOrDoctor(email: String, callback: (String) -> Unit) {
        firestore.collection("doctors").document(email).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    callback(doc.getString("name") ?: email)
                } else {
                    firestore.collection("users").document(email).get()
                        .addOnSuccessListener { doctorDoc ->
                            callback(doctorDoc.getString("name") ?: email)
                        }
                        .addOnFailureListener {
                            callback(email)
                        }
                }
            }
            .addOnFailureListener {
                callback(email)
            }
    }

    fun fetchParticipantInfo(email: String, callback: (String, String) -> Unit) {
        firestore.collection("admins").document(email).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    callback(doc.getString("name") ?: email, "")
                } else {
                    fetchDoctorOrUser(email, callback)
                }
            }
            .addOnFailureListener {
                fetchDoctorOrUser(email, callback)
            }
    }

    private fun fetchDoctorOrUser(email: String, callback: (String, String) -> Unit) {
        firestore.collection("doctors").document(email).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("name") ?: email
                    val specialization = doc.getString("specialization") ?: ""
                    callback(name, specialization)
                } else {
                    firestore.collection("users").document(email).get()
                        .addOnSuccessListener { userDoc ->
                            callback(userDoc.getString("name") ?: email, "")
                        }
                        .addOnFailureListener {
                            callback(email, "")
                        }
                }
            }
            .addOnFailureListener {
                callback(email, "")
            }
    }



}
