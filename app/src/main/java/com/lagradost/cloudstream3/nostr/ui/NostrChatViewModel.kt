package com.lagradost.cloudstream3.nostr.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.nostr.NostrConstants
import com.lagradost.cloudstream3.nostr.models.ChatMessage
import com.lagradost.cloudstream3.nostr.network.NostrRelayPool
import com.lagradost.cloudstream3.nostr.repositories.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class NostrChatViewModel(app: Application) : AndroidViewModel(app) {

    private val okHttpClient = OkHttpClient()
    private val relayPool = NostrRelayPool(okHttpClient, viewModelScope)
    private val chatRepository = ChatRepository(app, relayPool)

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        viewModelScope.launch {
            relayPool.connectToRelays(NostrConstants.DEFAULT_RELAYS)
            _isConnected.value = true
        }
    }

    fun loadChat(streamId: String, authorPubkey: String) {
        viewModelScope.launch {
            chatRepository.getChatMessages(streamId, authorPubkey)
                .catch { e ->
                    // Handle error silently or log
                }
                .collectLatest { messages ->
                    _chatMessages.value = messages
                }
        }
    }

    fun sendMessage(streamId: String, authorPubkey: String, content: String) {
        viewModelScope.launch {
            chatRepository.sendChatMessage(streamId, authorPubkey, content)
        }
    }

    override fun onCleared() {
        relayPool.disconnectAll()
        super.onCleared()
    }
}
