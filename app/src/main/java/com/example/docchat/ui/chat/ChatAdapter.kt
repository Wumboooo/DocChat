package com.example.docchat.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.docchat.Chat
import com.example.docchat.Message
import com.example.docchat.R
import java.text.DateFormat
import java.util.Date

class ChatAdapter(
    private val messages: List<Message>,
    private val onClick: ((Chat) -> Unit)? = null
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageTextView)
        private val timestamp: TextView = itemView.findViewById(R.id.timestampTextView)

        fun bind(message: Message) {
            messageText.text = message.text
            timestamp.text = DateFormat.getTimeInstance().format(Date(message.timestamp))
        }
    }
}
