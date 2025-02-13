package com.example.docchat.ui.chat

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.docchat.R
import com.example.docchat.SplashScreenActivity.Companion.globalRole
import com.example.docchat.ui.Messages
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class ChatActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var chatPartnerName: TextView
    private lateinit var chatStatus: TextView

    private val messages = mutableListOf<Messages>()
    private var chatId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        recyclerView = findViewById(R.id.recyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        chatPartnerName = findViewById(R.id.chatPartnerName)
        chatStatus = findViewById(R.id.chatStatus)

        val chatRepository = ChatRepository(firestore)
        val factory = ChatViewModelFactory(chatRepository)
        chatViewModel = ViewModelProvider(this, factory).get(ChatViewModel::class.java)

        chatId = intent.getStringExtra("chatId")
        if (chatId == null) {
            Log.e("ChatActivity", "Received invalid chat ID")
            Toast.makeText(this, "Chat ID tidak ditemukan.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        Log.d("ChatActivity", "Chat dibuka dengan ID: $chatId")

        recyclerView.layoutManager = LinearLayoutManager(this)

        val currentUserEmail = auth.currentUser?.email ?: return
        chatAdapter = ChatAdapter(messages, currentUserEmail)
        recyclerView.adapter = chatAdapter


        chatViewModel.messages.observe(this) { newMessages ->
            messages.clear()
            messages.addAll(newMessages)
            chatAdapter.notifyDataSetChanged()
            recyclerView.scrollToPosition(messages.size - 1)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(chatId.hashCode())

        chatViewModel.loadMessages(chatId!!)
        recyclerView.viewTreeObserver.addOnGlobalLayoutListener {
            if (messages.isNotEmpty()) {
                chatViewModel.markMessagesAsRead(chatId!!)
            }
        }

        updateFCMToken()

        val partnerName = intent.getStringExtra("partnerName")
        if (partnerName != null) {
            chatPartnerName.text = partnerName
        } else {
            Toast.makeText(this, "Nama tidak ditemukan.", Toast.LENGTH_SHORT).show()
            finish()
        }

        monitorChatStatus()

        sendButton.setOnClickListener {
            val text = messageEditText.text.toString()
            chatViewModel.sendMessage(chatId!!, currentUserEmail, text, false, this)
            messageEditText.text.clear()
        }

        observeActiveUsers()
    }

    override fun onResume() {
        super.onResume()
        chatViewModel.setUserActiveInChat(chatId!!, true)
    }

    override fun onPause() {
        super.onPause()
        chatViewModel.setUserActiveInChat(chatId!!, false)
    }

    private fun observeActiveUsers() {
        chatViewModel.monitorActiveParticipants(chatId!!) { activeParticipants ->
            Log.d("ChatActivity", "Active users: $activeParticipants")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        menu?.findItem(R.id.menu_forward)?.isVisible = globalRole == "admin"
        menu?.findItem(R.id.menu_end_session)?.isVisible = globalRole != "user"
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

    private fun updateFCMToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val user = FirebaseAuth.getInstance().currentUser
            user?.let {
                FirebaseFirestore.getInstance().collection("users")
                    .document(user.email!!)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            saveFCMToken("users", user.email!!, token)
                        } else {
                            FirebaseFirestore.getInstance().collection("doctors")
                                .document(user.email!!)
                                .get()
                                .addOnSuccessListener { doc ->
                                    if (doc.exists()) {
                                        saveFCMToken("doctors", user.email!!, token)
                                    } else {
                                        saveFCMToken("admins", user.email!!, token)
                                    }
                                }
                        }
                    }
            }
        }
    }

    private fun saveFCMToken(path: String, email: String, token: String) {
        FirebaseFirestore.getInstance().collection(path)
            .document(email)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d("Firebase", "FCM Token saved for $email")
            }
            .addOnFailureListener {
                Log.e("Firebase", "Failed to save FCM Token", it)
            }
    }

    private fun monitorChatStatus() {
        firestore.collection("chats").document(chatId!!)
            .addSnapshotListener { documentSnapshot, error ->
                if (error != null) {
                    Log.e("ChatActivity", "Gagal memantau status chat", error)
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val status = documentSnapshot.getString("status") ?: "error"
                    chatStatus.text = status
                }
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

                chatViewModel.sendMessage(newChatId, doctorEmail, "Ada yang bisa saya bantu?", true, this)

                Toast.makeText(this, "Chat diteruskan ke dokter.", Toast.LENGTH_SHORT).show()
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
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Close")
            .setMessage("Apakah Anda yakin ingin menghapus menutup obrolan?")
            .setPositiveButton("Hapus") { _, _ ->
                firestore.collection("chats").document(chatId!!)
                    .update("status", "closed")
                    .addOnSuccessListener {
                        Toast.makeText(this, "Sesi berakhir. Tidak bisa mengirim pesan lagi.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()

    }
}
