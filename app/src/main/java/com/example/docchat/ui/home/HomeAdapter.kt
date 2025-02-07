package com.example.docchat.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.docchat.R
import com.example.docchat.ui.Chat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeAdapter(
    private var chats: List<Chat>,
    private val onChatClick: (Chat) -> Unit,
    private val onDeleteClick: (Chat) -> Unit
) : RecyclerView.Adapter<HomeAdapter.HomeViewHolder>() {

    fun updateChats(newChats: List<Chat>) {
        chats = newChats
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return HomeViewHolder(view, onChatClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val chat = chats[position]
        holder.bind(chat)
    }

    override fun getItemCount(): Int = chats.size

    class HomeViewHolder(
        itemView: View,
        private val onChatClick: (Chat) -> Unit,
        private val onDeleteClick: (Chat) -> Unit // Tambahkan ini di HomeViewHolder
    ) : RecyclerView.ViewHolder(itemView) {

        private val chatNameTextView: TextView = itemView.findViewById(R.id.chatNameTextView)
        private val lastMessageTextView: TextView = itemView.findViewById(R.id.lastMessageTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        private val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)

        fun bind(chat: Chat) {
            val firestore = FirebaseFirestore.getInstance()
            val chatRepo = HomeRepository(firestore)

            chat.participantName?.let {
                chatRepo.fetchParticipantInfo(it) { name, specialization ->
                    val specializationText = if (specialization.isNotEmpty()) " - $specialization" else ""
                    chatNameTextView.text = "$name$specializationText - ${chat.status}"
                }
            }

            lastMessageTextView.text = chat.lastMessage

            val lastUpdatedDate = Date(chat.lastUpdated)
            val currentDate = Date()

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            val calendarLastUpdated = Calendar.getInstance().apply { time = lastUpdatedDate }
            val calendarCurrent = Calendar.getInstance().apply { time = currentDate }

            itemView.setOnClickListener { onChatClick(chat) }

            timestampTextView.text = if (calendarLastUpdated.get(Calendar.YEAR) == calendarCurrent.get(Calendar.YEAR) &&
                calendarLastUpdated.get(Calendar.DAY_OF_YEAR) == calendarCurrent.get(Calendar.DAY_OF_YEAR)
            ) {
                timeFormat.format(lastUpdatedDate)
            } else {
                dateFormat.format(lastUpdatedDate)
            }

            if (chat.status == "closed") {
                deleteButton.visibility = View.VISIBLE
                deleteButton.setOnClickListener { onDeleteClick(chat) }
            } else {
                deleteButton.visibility = View.GONE
            }
        }
    }
}
