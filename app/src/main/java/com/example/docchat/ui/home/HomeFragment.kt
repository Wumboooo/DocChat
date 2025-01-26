package com.example.docchat.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.docchat.Chat
import com.example.docchat.Message
import com.example.docchat.R
import com.example.docchat.databinding.FragmentHomeBinding
import com.example.docchat.ui.chat.ChatActivity
import com.example.docchat.ui.chat.ChatAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
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

        chatAdapter = ChatAdapter(chatList) { chat ->
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("chatId", chat.id)
            startActivity(intent)
        }

        recyclerView.adapter = chatAdapter
        loadChats()
        return view
    }

    private fun loadChats() {
        val currentUser = auth.currentUser ?: return
        firestore.collection("chats")
            .whereArrayContains("participants", currentUser.uid)
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    chatList.clear()
                    for (doc in snapshot.documents) {
                        val chat = doc.toObject(Chat::class.java)
                        if (chat != null) {
                            chatList.add(chat)
                        }
                    }
                    chatAdapter.notifyDataSetChanged()
                }
            }
    }
}

