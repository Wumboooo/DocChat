package com.example.docchat.ui.chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.docchat.R
import com.example.docchat.ui.Message
import com.example.docchat.ui.login.LoginActivity.Companion.globalRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var chatViewModel: ChatViewModel

    private val messages = mutableListOf<Message>()
    private var chatId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // **Pastikan Firestore sudah diinisialisasi sebelum digunakan**
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        recyclerView = findViewById(R.id.recyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)

        // **Baru inisialisasi repository dan ViewModel setelah firestore siap**
        val chatRepository = ChatRepository(firestore)
        val factory = ChatViewModelFactory(chatRepository)
        chatViewModel = ViewModelProvider(this, factory).get(ChatViewModel::class.java)

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

        chatViewModel.messages.observe(this) { newMessages ->
            messages.clear()
            messages.addAll(newMessages)
            chatAdapter.notifyDataSetChanged()
            recyclerView.scrollToPosition(messages.size - 1)
        }

        chatViewModel.loadMessages(chatId!!)

        sendButton.setOnClickListener {
            val text = messageEditText.text.toString()
            chatViewModel.sendMessage(chatId!!, auth, text)
            messageEditText.text.clear()
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

    private fun forwardMessage() {
        chatViewModel.forwardMessage(firestore, chatId!!) { success, doctorList ->
            if (!success || doctorList == null) {
                Toast.makeText(this, "Tidak ada dokter tersedia.", Toast.LENGTH_SHORT).show()
                return@forwardMessage
            }

            ChatHelper.showDoctorSelectionDialog(this, doctorList) { doctorEmail ->
                createNewChatWithDoctor(doctorEmail)
            }
        }
    }

    private fun createNewChatWithDoctor(doctorEmail: String) {
        // Mendapatkan email dari participants array di chatId saat ini
        val chatRef = firestore.collection("chats").document(chatId!!)
        chatRef.get().addOnSuccessListener { document ->
            val participants = document.get("participants") as? List<*>
            val userEmail = participants?.get(0) ?: return@addOnSuccessListener

            val newChat = mapOf(
                "participants" to listOf(userEmail, doctorEmail),
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
        ChatHelper.showSummaryDialog(this) { disease, medicine ->
            saveSummary(disease, medicine)
        }
    }

    private fun saveSummary(disease: String, medicine: String) {
        val chatRef = firestore.collection("chats").document(chatId!!)
        chatRef.get().addOnSuccessListener { document ->
            val participants = document.get("participants") as? List<*> ?: return@addOnSuccessListener
            val userEmail = participants.getOrNull(0) ?: return@addOnSuccessListener
            val doctorEmail = participants.getOrNull(1) as? String ?: return@addOnSuccessListener
            val currentEmail = auth.currentUser?.email ?: return@addOnSuccessListener

            val userRef = firestore.collection("doctors").document(doctorEmail)
            userRef.get().addOnSuccessListener { doc ->
                val doctorName = doc.getString("name") ?: return@addOnSuccessListener

                val summaryData = mapOf(
                    "patientEmail" to userEmail,
                    "doctorName" to doctorName,
                    "date" to System.currentTimeMillis(),
                    "disease" to disease,
                    "medicine" to medicine
                )

                chatRef.update("summary", summaryData)
                    .addOnSuccessListener {
                        val summaryMessage = mapOf(
                            "senderEmail" to currentEmail,
                            "text" to "Dokter telah membuat summary.",
                            "timestamp" to System.currentTimeMillis()
                        )

                        chatRef.collection("messages").add(summaryMessage)
                        Toast.makeText(this, "Ringkasan berhasil dibuat.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal membuat ringkasan.", Toast.LENGTH_SHORT).show()
                    }
            }
        }
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
