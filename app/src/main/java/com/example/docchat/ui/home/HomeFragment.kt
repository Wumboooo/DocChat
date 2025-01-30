package com.example.docchat.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.docchat.R
import com.example.docchat.ui.chat.ChatActivity
import com.example.docchat.ui.login.LoginActivity.Companion.globalRole
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: HomeAdapter
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val repository = ChatRepository(firestore)

        viewModel = ViewModelProvider(
            this,
            ViewModelFactory(repository)
        )[HomeViewModel::class.java]

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        chatAdapter = HomeAdapter(emptyList()) { chat ->
            openChat(chat.chatId)
        }
        recyclerView.adapter = chatAdapter

        viewModel.chats.observe(viewLifecycleOwner) { chats ->
            chatAdapter.updateChats(chats)
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }

        loadChats()

        view.findViewById<FloatingActionButton>(R.id.startChat).setOnClickListener {
            startChatWithAdmin()
        }

        return view
    }

    private fun loadChats() {
        val currentUser = auth.currentUser ?: return
        val currentEmail = currentUser.email ?: return
        viewModel.loadChats(currentEmail)
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

    private fun openChat(chatId: String) {
        val intent = Intent(context, ChatActivity::class.java)
        intent.putExtra("chatId", chatId)
        startActivity(intent)
    }
}
