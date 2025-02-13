package com.example.docchat.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.docchat.R
import com.example.docchat.ui.Messages
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val messages: List<Messages>,
    private val currentUserEmail: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENDER = 1
        private const val VIEW_TYPE_RECEIVER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderEmail == currentUserEmail) VIEW_TYPE_SENDER else VIEW_TYPE_RECEIVER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SENDER) {
            SenderViewHolder(inflater.inflate(R.layout.item_message_sender, parent, false))
        } else {
            ReceiverViewHolder(inflater.inflate(R.layout.item_message_receiver, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is SenderViewHolder) {
            holder.bind(message)
        } else if (holder is ReceiverViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    class SenderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)

        fun bind(message: Messages) {
            timestampTextView.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

            if (message.imageUrl != null) {
                messageTextView.visibility = View.GONE
            } else {
                messageTextView.visibility = View.VISIBLE
                messageTextView.text = message.text
            }
        }
    }

    class ReceiverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)

        fun bind(message: Messages) {
            timestampTextView.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

            if (message.imageUrl != null) {
                messageTextView.visibility = View.GONE
            } else {
                messageTextView.visibility = View.VISIBLE
                messageTextView.text = message.text
            }
        }
    }
}

