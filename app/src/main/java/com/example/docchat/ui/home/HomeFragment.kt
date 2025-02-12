package com.example.docchat.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.docchat.R
import com.example.docchat.SplashScreenActivity.Companion.globalRole
import com.example.docchat.ui.Chat
import com.example.docchat.ui.chat.ChatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: HomeAdapter
    private lateinit var startChatButton: FloatingActionButton
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        arguments?.getString("chatId")?.let { chatId ->
            openChat(chatId, "Unknown", "active")
        }

        auth = FirebaseAuth.getInstance()
        val repository = HomeRepository(firestore)

        viewModel = ViewModelProvider(
            this,
            ViewModelFactory(repository)
        )[HomeViewModel::class.java]

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        chatAdapter = HomeAdapter(emptyList(),
            { chat -> openChat(chat.chatId, chat.participantName, chat.status) },
            { chat -> deleteChat(chat) }
        )
        recyclerView.adapter = chatAdapter

        viewModel.chats.observe(viewLifecycleOwner) { chats ->
            val currentEmail = auth.currentUser?.email ?: return@observe

            val filteredChats = chats.filter { chat ->
                !(chat.archivedBy.contains(currentEmail))
            }
            chatAdapter.updateChats(filteredChats)
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }

        loadChats()

        startChatButton = view.findViewById(R.id.startChat)

        if (globalRole == "admin") {
            startChatButton.visibility = View.GONE
        } else {
            startChatButton.visibility = View.VISIBLE
        }

        startChatButton.setOnClickListener {
            startChatWithAdmin()
        }

        viewModel.unreadMessagesCount.observe(viewLifecycleOwner) { count ->
            updateUnreadBadge(count)
            chatAdapter.updateUnreadCounts(mapOf("global_unread" to count)) // Sesuaikan dengan ID chat jika perlu
        }

        val userEmail = auth.currentUser?.email
        userEmail?.let {
            viewModel.listenForUnreadMessages(it)
        }

        return view
    }

    private fun updateUnreadBadge(count: Int) {
        Log.d("HomeFragment", "Updating badge with count: $count")
        val badgeTextView = view?.findViewById<TextView>(R.id.chatBadge)
        badgeTextView?.visibility = if (count > 0) View.VISIBLE else View.GONE
        badgeTextView?.text = count.toString()
    }


    private fun openChat(chatId: String, partnerName: String, status: String) {
        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra("chatId", chatId)
            putExtra("partnerName", partnerName)
            putExtra("status", status)
        }
        startActivity(intent)
    }

    private fun loadChats() {
        val currentUser = auth.currentUser ?: return
        val currentEmail = currentUser.email ?: return
        viewModel.loadChats(currentEmail)
    }

    private fun startChatWithAdmin() {
        val currentUser = auth.currentUser ?: return
        val currentEmail = currentUser.email ?: return

        firestore.collection("chats")
            .whereArrayContains("participants", currentEmail)
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { chatQuery ->
                if (!chatQuery.isEmpty) {
                    openChat(chatQuery.documents.first().id)
                } else {
                    assignAdminAndCreateChat(currentEmail)
                }
            }
    }

    private fun assignAdminAndCreateChat(currentEmail: String) {
        firestore.collection("admins")
            .get()
            .addOnSuccessListener { adminQuery ->
                val admins = adminQuery.documents.map { it.id }

                if (admins.isEmpty()) {
                    Toast.makeText(context, "Tidak ada admin tersedia.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                firestore.collection("chats")
                    .whereEqualTo("status", "active")
                    .get()
                    .addOnSuccessListener { chatQuery ->
                        val adminChatCount = mutableMapOf<String, Int>()

                        // Inisialisasi jumlah chat aktif setiap admin
                        admins.forEach { adminChatCount[it] = 0 }

                        // Hitung jumlah chat aktif per admin
                        chatQuery.documents.forEach { doc ->
                            val participants = doc.get("participants") as? List<String> ?: emptyList()
                            participants.forEach { participant ->
                                if (admins.contains(participant)) {
                                    adminChatCount[participant] = adminChatCount.getOrDefault(participant, 0) + 1
                                }
                            }
                        }

                        // Pilih admin dengan jumlah chat aktif paling sedikit
                        val minChats = adminChatCount.values.minOrNull() ?: 0
                        val leastBusyAdmins = adminChatCount.filterValues { it == minChats }.keys

                        val selectedAdmin = leastBusyAdmins.random() // Pilih secara acak jika ada beberapa admin dengan jumlah chat yang sama

                        createNewChat(currentEmail, selectedAdmin)
                    }
            }
    }

    private fun createNewChat(currentEmail: String, adminEmail: String) {
        val participants = listOf(currentEmail, adminEmail)

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

    private fun deleteChat(chat: Chat) {
        val currentUser = auth.currentUser ?: return
        val currentEmail = currentUser.email ?: return

        firestore.collection("chats").document(chat.chatId)
            .update("archivedBy", FieldValue.arrayUnion(currentEmail)) // Tambahkan email ke archivedBy
            .addOnSuccessListener {
                Toast.makeText(context, "Chat diarsipkan", Toast.LENGTH_SHORT).show()
                loadChats() // Refresh daftar chat setelah "penghapusan"
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal mengarsipkan chat", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openChat(chatId: String) {
        val intent = Intent(context, ChatActivity::class.java)
        intent.putExtra("chatId", chatId)
        startActivity(intent)
    }
}
