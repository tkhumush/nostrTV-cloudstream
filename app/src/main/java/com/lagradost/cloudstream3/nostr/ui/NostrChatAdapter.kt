package com.lagradost.cloudstream3.nostr.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.nostr.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

class NostrChatAdapter : RecyclerView.Adapter<NostrChatAdapter.ViewHolder>() {

    private var messages = emptyList<ChatMessage>()
    private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun submitList(newMessages: List<ChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val authorName: TextView = itemView.findViewById(R.id.authorName)
        private val messageContent: TextView = itemView.findViewById(R.id.messageContent)
        private val timestamp: TextView = itemView.findViewById(R.id.timestamp)

        fun bind(message: ChatMessage) {
            // Show shortened pubkey as author name (first 8 chars)
            authorName.text = message.authorPubkey.take(8)
            messageContent.text = message.content

            // Format timestamp
            val date = Date(message.createdAt * 1000)
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            timestamp.text = dateFormat.format(date)
        }
    }
}
