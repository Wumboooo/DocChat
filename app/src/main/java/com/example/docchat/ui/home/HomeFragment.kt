package com.example.docchat.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.docchat.Chat
import com.example.docchat.R
import com.example.docchat.ui.chat.ChatActivity
import com.example.docchat.ui.login.LoginActivity.Companion.globalRole
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: HomeAdapter
    private val chatList = mutableListOf<Chat>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        chatAdapter = HomeAdapter(chatList) { chat ->
            Log.d("HomeFragment", "Opening chat with ID: ${chat.chatId}")
            if (chat.chatId.isNullOrBlank()) {
                Toast.makeText(context, "Chat ID tidak valid.", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(context, ChatActivity::class.java)
                intent.putExtra("chatId", chat.chatId)
                startActivity(intent)
            }
        }

        recyclerView.adapter = chatAdapter
        loadChats()

        view.findViewById<FloatingActionButton>(R.id.startChat).setOnClickListener {
            if (globalRole == "user") {
                startChatWithAdmin()
            } else {
                Toast.makeText(context, "Only users can start chats.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun loadChats() {
        val currentUser = auth.currentUser ?: return
        val currentEmail = currentUser.email ?: return

        firestore.collection("chats")
            .whereArrayContains("participants", currentEmail)
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HomeFragment", "Error fetching chats: ${error.message}")
                    return@addSnapshotListener
                }
                chatList.clear()
                snapshot?.documents?.forEach { doc ->
                    val chat = doc.toObject(Chat::class.java)
                    if (chat != null) {
                        chat.chatId = doc.id
                        val otherEmail = chat.participants.firstOrNull { it != currentEmail }
                        if (otherEmail != null) {
                            getParticipantName(otherEmail) { name ->
                                chat.participantName = name
                                chatAdapter.notifyDataSetChanged()
                            }
                        }
                        chatList.add(chat)
                    }
                }
                chatAdapter.notifyDataSetChanged()
            }
    }

    private fun startChatWithAdmin() {
        val currentUser = auth.currentUser ?: return
        val currentEmail = currentUser.email ?: return

        firestore.collection("admins")
            .limit(1)
            .get()
            .addOnSuccessListener { adminQuery ->
                if (!adminQuery.isEmpty) {
                    val adminEmail = adminQuery.documents.first().id
                    if (adminEmail.isEmpty()) {
                        Toast.makeText(context, "Admin email not found.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val participants = listOf(currentEmail, adminEmail).sorted()

                    firestore.collection("chats")
                        .whereArrayContains("participants", currentEmail)
                        .get()
                        .addOnSuccessListener { chatQuery ->
                            val existingChat = chatQuery.documents.find { doc ->
                                val chatParticipants = (doc.get("participants") as? List<*>)?.map { it.toString() }?.sorted()
                                chatParticipants == participants
                            }
                            if (existingChat != null) {
                                openChat(existingChat.id)
                            } else {
                                val newChat = mapOf(
                                    "participants" to participants,
                                    "lastMessage" to "Start of chat",
                                    "lastUpdated" to System.currentTimeMillis(),
                                    "status" to "active"
                                )
                                firestore.collection("chats")
                                    .add(newChat)
                                    .addOnSuccessListener { docRef ->
                                        openChat(docRef.id)
                                    }
                            }
                        }
                } else {
                    Toast.makeText(context, "No admin available.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun getParticipantName(email: String, callback: (String) -> Unit) {
        Log.d("HomeFragment", "Fetching name for email: $email")
        firestore.collection("doctors").document(email).get()
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {
                    callback(userDoc.getString("name") ?: email)
                } else {
                    fetchFromAdminOrDoctor(email, callback)
                }
            }
            .addOnFailureListener {
                Log.e("HomeFragment", "Error fetching from users: ${it.message}")
                fetchFromAdminOrDoctor(email, callback)
            }
    }

    private fun fetchFromAdminOrDoctor(email: String, callback: (String) -> Unit) {
        firestore.collection("admins").document(email).get()
            .addOnSuccessListener { adminDoc ->
                if (adminDoc.exists()) {
                    callback(adminDoc.getString("name") ?: email)
                } else {
                    firestore.collection("users").document(email).get()
                        .addOnSuccessListener { doctorDoc ->
                            if (doctorDoc.exists()) {
                                callback(doctorDoc.getString("name") ?: email)
                            } else {
                                callback(email) // Default fallback
                            }
                        }
                        .addOnFailureListener {
                            Log.e("HomeFragment", "Error fetching from doctors: ${it.message}")
                            callback(email)
                        }
                }
            }
            .addOnFailureListener {
                Log.e("HomeFragment", "Error fetching from admins: ${it.message}")
                callback(email)
            }
    }

    private fun openChat(chatId: String) {
        val intent = Intent(context, ChatActivity::class.java)
        intent.putExtra("chatId", chatId)
        startActivity(intent)
    }
}
