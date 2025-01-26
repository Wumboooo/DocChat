package com.example.docchat.ui.chat

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.docchat.Message
import com.example.docchat.R
import com.example.docchat.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button

    private val messages = mutableListOf<Message>()
    private var chatId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        recyclerView = findViewById(R.id.recyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)

        chatId = intent.getStringExtra("chatId")
        recyclerView.layoutManager = LinearLayoutManager(this)

        chatAdapter = ChatAdapter(messages)
        recyclerView.adapter = chatAdapter

        loadMessages()

        sendButton.setOnClickListener {
            sendMessage()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_forward -> {
                forwardMessage()
                true
            }
            R.id.menu_end_session -> {
                endSession()
                true
            }
            R.id.menu_summary -> {
                createSummary()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadMessages() {
        val chatRef = firestore.collection("chats").document(chatId!!)
        chatRef.collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    messages.clear()
                    for (doc in snapshot.documents) {
                        val message = doc.toObject(Message::class.java)
                        if (message != null) {
                            messages.add(message)
                        }
                    }
                    chatAdapter.notifyDataSetChanged()
                    recyclerView.scrollToPosition(messages.size - 1)
                }
            }
    }

    private fun sendMessage() {
        val text = messageEditText.text.toString()
        if (text.isBlank()) return

        val message = mapOf(
            "senderId" to auth.currentUser!!.uid,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )

        val chatRef = firestore.collection("chats").document(chatId!!)
        chatRef.collection("messages").add(message).addOnSuccessListener {
            chatRef.update(
                mapOf(
                    "lastMessage" to text,
                    "lastUpdated" to System.currentTimeMillis()
                )
            )
            messageEditText.text.clear()
        }
    }

    private fun createSummary() {
        val summary = "Summary dari dokter."
        val message = mapOf(
            "senderId" to auth.currentUser!!.uid,
            "text" to summary,
            "timestamp" to System.currentTimeMillis(),
            "isSummary" to true
        )

        firestore.collection("chats").document(chatId!!).collection("messages")
            .add(message).addOnSuccessListener {
                Toast.makeText(this, "Summary dibuat.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun endSession() {
        firestore.collection("chats").document(chatId!!)
            .update("status", "closed")
            .addOnSuccessListener {
                Toast.makeText(this, "Sesi berakhir.", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
}
