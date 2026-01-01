package com.lagradost.cloudstream3.nostr.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.nostr.models.Stream

class NostrStreamAdapter : RecyclerView.Adapter<NostrStreamAdapter.StreamViewHolder>() {

    private var streams = emptyList<Stream>()
    private var onItemClickListener: ((Stream) -> Unit)? = null

    fun submitList(newStreams: List<Stream>) {
        streams = newStreams
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (Stream) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nostr_stream, parent, false)
        return StreamViewHolder(view)
    }

    override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
        val stream = streams[position]
        holder.bind(stream)
    }

    override fun getItemCount(): Int = streams.size

    inner class StreamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.streamThumbnail)
        private val liveIndicator: TextView = itemView.findViewById(R.id.liveIndicator)
        private val participantCount: TextView = itemView.findViewById(R.id.participantCount)
        private val title: TextView = itemView.findViewById(R.id.streamTitle)
        private val summary: TextView = itemView.findViewById(R.id.streamSummary)

        fun bind(stream: Stream) {
            // Load thumbnail
            thumbnail.load(stream.image) {
                crossfade(true)
                placeholder(com.lagradost.cloudstream3.utils.getImageFromDrawable(itemView.context, R.drawable.ic_refresh))
                error(com.lagradost.cloudstream3.utils.getImageFromDrawable(itemView.context, R.drawable.ic_refresh))
            }

            // Show LIVE indicator
            liveIndicator.visibility = View.VISIBLE

            // Show participant count if available
            if (stream.currentParticipants != null && stream.currentParticipants > 0) {
                participantCount.text = "👤 ${stream.currentParticipants}"
                participantCount.visibility = View.VISIBLE
            } else {
                participantCount.visibility = View.GONE
            }

            // Set title and summary
            title.text = stream.title
            summary.text = stream.summary ?: ""
            summary.visibility = if (stream.summary.isNullOrBlank()) View.GONE else View.VISIBLE

            // Click listener
            itemView.setOnClickListener {
                onItemClickListener?.invoke(stream)
            }
        }
    }
}
