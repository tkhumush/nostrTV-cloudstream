package com.lagradost.cloudstream3.nostr.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentNostrStreamsBinding
import com.lagradost.cloudstream3.ui.player.ExtractorLinkGenerator
import com.lagradost.cloudstream3.ui.player.GeneratorPlayer
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.UIHelper.navigate
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NostrStreamsFragment : Fragment() {

    private var _binding: FragmentNostrStreamsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NostrStreamsViewModel by viewModels()
    private val adapter = NostrStreamAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNostrStreamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = GridLayoutManager(context, 2)
        binding.recyclerView.adapter = adapter

        adapter.setOnItemClickListener { stream ->
            if (stream.streamingUrl.isNullOrBlank()) {
                Toast.makeText(
                    requireContext(),
                    "Stream URL not available",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnItemClickListener
            }

            playNostrStream(stream)
        }
    }

    private fun playNostrStream(stream: com.lagradost.cloudstream3.nostr.models.Stream) {
        val streamUrl = stream.streamingUrl ?: return

        lifecycleScope.launch {
            // Use newExtractorLink which properly handles audio and video
            val extractorLink = com.lagradost.cloudstream3.utils.newExtractorLink(
                source = "Nostr",
                name = stream.title,
                url = streamUrl,
                type = com.lagradost.cloudstream3.utils.INFER_TYPE, // Auto-detect M3U8/DASH/VIDEO from URL
            ) {
                // Set referer if needed (empty for most Nostr streams)
                this.referer = ""
                // Use Unknown quality for live streams
                this.quality = com.lagradost.cloudstream3.utils.Qualities.Unknown.value
            }

            // Create a generator with the link
            val generator = ExtractorLinkGenerator(
                links = listOf(extractorLink),
                subtitles = emptyList()
            )

            // Navigate to the player with Nostr metadata
            val bundle = GeneratorPlayer.newInstance(generator).apply {
                putString("nostr_stream_id", stream.id)
                putString("nostr_author_pubkey", stream.authorPubkey)
                putString("nostr_stream_title", stream.title)
                putBoolean("is_nostr_stream", true)
            }

            activity?.navigate(
                R.id.global_to_navigation_player,
                bundle
            )
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is NostrStreamsViewModel.UiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.emptyText.visibility = View.GONE
                        binding.recyclerView.visibility = View.GONE
                    }
                    is NostrStreamsViewModel.UiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.emptyText.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                        adapter.submitList(state.streams)
                    }
                    is NostrStreamsViewModel.UiState.Empty -> {
                        binding.progressBar.visibility = View.GONE
                        binding.emptyText.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    }
                    is NostrStreamsViewModel.UiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.emptyText.visibility = View.VISIBLE
                        binding.emptyText.text = state.message
                        binding.recyclerView.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
