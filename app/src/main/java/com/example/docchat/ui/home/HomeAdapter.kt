package com.example.docchat.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.docchat.R
import com.example.docchat.ui.Chat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeAdapter(
    private var chats: List<Chat>,
    private val onChatClick: (Chat) -> Unit
) : RecyclerView.Adapter<HomeAdapter.HomeViewHolder>() {

    fun updateChats(newChats: List<Chat>) {
        chats = newChats
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return HomeViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val chat = chats[position]
        holder.bind(chat)
        holder.itemView.setOnClickListener { onChatClick(chat) }
    }

    override fun getItemCount(): Int = chats.size

    class HomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chatNameTextView: TextView = itemView.findViewById(R.id.chatNameTextView)
        private val lastMessageTextView: TextView = itemView.findViewById(R.id.lastMessageTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)

        fun bind(chat: Chat) {
            val firestore = FirebaseFirestore.getInstance()
            val chatRepo = HomeRepository(firestore)

            chat.participantName?.let {
                chatRepo.fetchParticipantInfo(it!!) { name, specialization ->
                    val specializationText = if (specialization.isNotEmpty()) " - $specialization" else ""
                    chatNameTextView.text = "$name$specializationText - ${chat.status}"
                }
            }

            lastMessageTextView.text = chat.lastMessage
            timestampTextView.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(chat.lastUpdated))
        }

    }
}
