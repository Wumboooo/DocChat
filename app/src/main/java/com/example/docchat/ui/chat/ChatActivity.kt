package com.example.docchat.ui.chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.docchat.Message
import com.example.docchat.R
import com.example.docchat.ui.login.LoginActivity.Companion.globalRole
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

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        recyclerView = findViewById(R.id.recyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)

        chatId = intent.getStringExtra("chatId")
        if (chatId == null) {
            Log.e("ChatActivity", "Received invalid chat ID")
            Toast.makeText(this, "Chat ID is missing.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val currentUserEmail = auth.currentUser?.email ?: ""
        recyclerView.layoutManager = LinearLayoutManager(this)
        chatAdapter = ChatAdapter(messages, currentUserEmail)
        recyclerView.adapter = chatAdapter

        loadMessages()

        sendButton.setOnClickListener {
            sendMessage()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        menu?.findItem(R.id.menu_forward)?.isVisible = globalRole == "admin"
        menu?.findItem(R.id.menu_end_session)?.isVisible = globalRole == "doctor"
        menu?.findItem(R.id.menu_summary)?.isVisible = globalRole == "doctor"
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
                            Log.d("ChatActivity", "Message: ${message.text}, Sender: ${message.senderEmail}")
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
            "senderEmail" to auth.currentUser!!.email,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )

        val chatRef = firestore.collection("chats").document(chatId!!)
        chatRef.collection("messages").add(message).addOnSuccessListener { documentRef ->
            documentRef.get().addOnSuccessListener { snapshot ->
                val timestamp = snapshot.getLong("timestamp") ?: System.currentTimeMillis()
                chatRef.update(
                    mapOf(
                        "lastMessage" to text,
                        "lastUpdated" to timestamp
                    )
                )
                messageEditText.text.clear()
            }
        }
    }


    private fun forwardMessage() {
        val doctorsRef = firestore.collection("doctors")
        doctorsRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                Toast.makeText(this, "Tidak ada dokter tersedia.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            val doctorList = snapshot.documents.map { doc ->
                val name = doc.getString("name") ?: "Unknown"
                val specialization = doc.getString("specialization") ?: "Unknown"
                "$name - $specialization"
            }

            AlertDialog.Builder(this)
                .setTitle("Pilih Dokter")
                .setItems(doctorList.toTypedArray()) { _, which ->
                    val selectedDoctor = snapshot.documents[which]
                    val doctorEmail = selectedDoctor.id
                    createNewChatWithDoctor(doctorEmail)
                }
                .show()
        }
    }

    private fun createNewChatWithDoctor(doctorEmail: String) {
        // Mendapatkan email dari participants array di chatId saat ini
        val chatRef = firestore.collection("chats").document(chatId!!)
        chatRef.get().addOnSuccessListener { document ->
            val participants = document.get("participants") as? List<*>
            val senderEmail = participants?.get(0) ?: return@addOnSuccessListener

            val newChat = mapOf(
                "participants" to listOf(senderEmail, doctorEmail),
                "lastMessage" to "Ada yang bisa saya bantu?",
                "lastUpdated" to System.currentTimeMillis(),
                "timestamp" to System.currentTimeMillis(),
                "status" to "active"
            )

            firestore.collection("chats").add(newChat).addOnSuccessListener { documentRef ->
                val newChatId = documentRef.id

                val initialMessage = mapOf(
                    "senderEmail" to doctorEmail,
                    "text" to "Ada yang bisa saya bantu?",
                    "timestamp" to System.currentTimeMillis()
                )

                firestore.collection("chats").document(newChatId).collection("messages")
                    .add(initialMessage).addOnSuccessListener {
                        if (globalRole == "user") {
                            val intent = Intent(this, ChatActivity::class.java)
                            intent.putExtra("chatId", newChatId)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "Chat diteruskan ke dokter.", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
            }
        }
    }

    private fun createSummary() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_summary, null)
        val penyakitEditText = dialogView.findViewById<EditText>(R.id.penyakitEditText)
        val resepEditText = dialogView.findViewById<EditText>(R.id.resepEditText)

        AlertDialog.Builder(this)
            .setTitle("Buat Ringkasan")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val penyakit = penyakitEditText.text.toString()
                val resep = resepEditText.text.toString()

                if (penyakit.isBlank() || resep.isBlank()) {
                    Toast.makeText(this, "Harap isi semua field.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val summaryData = mapOf(
                    "penyakit" to penyakit,
                    "resep" to resep
                )

                firestore.collection("chats").document(chatId!!)
                    .update("summary", summaryData)
                    .addOnSuccessListener {
                        val summaryMessage = mapOf(
                            "senderId" to auth.currentUser!!.uid,
                            "text" to "Dokter telah membuat ringkasan.",
                            "timestamp" to System.currentTimeMillis(),
                            "isSummary" to true
                        )

                        firestore.collection("chats").document(chatId!!).collection("messages")
                            .add(summaryMessage)

                        Toast.makeText(this, "Ringkasan dibuat dan diteruskan ke admin.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal membuat ringkasan. Coba lagi.", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun endSession() {
        firestore.collection("chats").document(chatId!!)
            .update("status", "closed")
            .addOnSuccessListener {
                Toast.makeText(this, "Sesi berakhir. Tidak bisa mengirim pesan lagi.", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
}