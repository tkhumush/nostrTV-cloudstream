package com.lagradost.cloudstream3.nostr.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.nostr.models.Stream
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Helper class to add Nostr chat overlay to the video player
 */
object NostrChatOverlayHelper {

    /**
     * Adds chat overlay to the player container
     * @param playerContainer The root container of the player
     * @param lifecycleOwner Lifecycle owner for observing ViewModel
     * @param viewModel Chat view model
     * @param stream The Nostr stream being played
     */
    fun addChatOverlay(
        playerContainer: ViewGroup,
        lifecycleOwner: LifecycleOwner,
        viewModel: NostrChatViewModel,
        stream: Stream
    ): View? {
        val context = playerContainer.context

        // Inflate chat overlay
        val chatOverlay = LayoutInflater.from(context)
            .inflate(R.layout.nostr_chat_overlay, playerContainer, false)

        // Add to player container
        playerContainer.addView(chatOverlay)

        // Setup RecyclerView
        val chatRecyclerView = chatOverlay.findViewById<RecyclerView>(R.id.chatRecyclerView)
        val chatAdapter = NostrChatAdapter()
        chatRecyclerView.layoutManager = LinearLayoutManager(context)
        chatRecyclerView.adapter = chatAdapter

        // Setup input
        val chatInput = chatOverlay.findViewById<EditText>(R.id.chatInput)
        val sendButton = chatOverlay.findViewById<ImageButton>(R.id.chatSendButton)

        sendButton.setOnClickListener {
            val message = chatInput.text.toString().trim()
            if (message.isNotEmpty()) {
                viewModel.sendMessage(stream.id, stream.authorPubkey, message)
                chatInput.setText("")
            }
        }

        // Observe chat messages
        lifecycleOwner.lifecycleScope.launch {
            viewModel.chatMessages.collectLatest { messages ->
                chatAdapter.submitList(messages)
                // Auto-scroll to bottom
                if (messages.isNotEmpty()) {
                    chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
        }

        // Load chat for this stream
        viewModel.loadChat(stream.id, stream.authorPubkey)

        // Show the overlay
        chatOverlay.visibility = View.VISIBLE

        return chatOverlay
    }

    /**
     * Removes chat overlay from player
     */
    fun removeChatOverlay(chatOverlay: View?) {
        chatOverlay?.let {
            (it.parent as? ViewGroup)?.removeView(it)
        }
    }
}
