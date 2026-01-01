package com.lagradost.cloudstream3.nostr.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.nostr.NostrConstants
import com.lagradost.cloudstream3.nostr.models.Stream
import com.lagradost.cloudstream3.nostr.network.NostrRelayPool
import com.lagradost.cloudstream3.nostr.repositories.StreamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class NostrStreamsViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "NostrStreamsVM"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val relayPool = NostrRelayPool(okHttpClient, viewModelScope)
    private val streamRepository = StreamRepository(relayPool)

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _streams = MutableStateFlow<List<Stream>>(emptyList())
    val streams: StateFlow<List<Stream>> = _streams.asStateFlow()

    init {
        connectToRelays()  // loadStreams() is now called from within connectToRelays()
    }

    private fun connectToRelays() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Connecting to ${NostrConstants.DEFAULT_RELAYS.size} relays...")
                NostrConstants.DEFAULT_RELAYS.forEach { relay ->
                    Log.d(TAG, "Relay: $relay")
                }
                relayPool.connectToRelays(NostrConstants.DEFAULT_RELAYS)
                Log.d(TAG, "Successfully connected to relays")

                // IMPORTANT: Load streams AFTER relays are connected
                loadStreams()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to relays", e)
                _uiState.value = UiState.Error("Failed to connect to relays: ${e.message}")
            }
        }
    }

    private fun loadStreams() {
        // Note: This is now called from connectToRelays(), so it runs on the same coroutine
        Log.d(TAG, "Starting to load streams from relays...")
        viewModelScope.launch(Dispatchers.IO) {
            streamRepository.getLiveStreams()
                .catch { e ->
                    Log.e(TAG, "Error loading streams", e)
                    _uiState.value = UiState.Error("Error loading streams: ${e.message}")
                }
                .collectLatest { streamList ->
                    Log.d(TAG, "Received ${streamList.size} streams from relays")
                    streamList.forEach { stream ->
                        Log.d(TAG, "Stream: ${stream.title} (${stream.status}) - ${stream.streamingUrl}")
                    }
                    _streams.value = streamList
                    _uiState.value = if (streamList.isEmpty()) {
                        Log.d(TAG, "No live streams found")
                        UiState.Empty
                    } else {
                        Log.d(TAG, "Successfully loaded ${streamList.size} streams")
                        UiState.Success(streamList)
                    }
                }
        }
    }

    fun refresh() {
        _uiState.value = UiState.Loading
        streamRepository.clearCache()
        loadStreams()
    }

    override fun onCleared() {
        super.onCleared()
        relayPool.disconnectAll()
        streamRepository.closeSubscription()
    }

    sealed class UiState {
        object Loading : UiState()
        object Empty : UiState()
        data class Success(val streams: List<Stream>) : UiState()
        data class Error(val message: String) : UiState()
    }
}
