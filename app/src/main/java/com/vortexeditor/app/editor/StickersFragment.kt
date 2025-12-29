package com.vortexeditor.app.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vortexeditor.app.R

class StickersFragment : BottomSheetDialogFragment() {

    var onStickerSelected: ((String) -> Unit)? = null

    private val emojis = listOf(
        // Smileys
        "😀", "😃", "😄", "😁", "😅", "😂", "🤣", "😊",
        "😇", "🙂", "😉", "😍", "🥰", "😘", "😋", "😎",
        "🤩", "🥳", "😏", "😒", "😞", "😢", "😭", "😤",
        // Gestures
        "👍", "👎", "👏", "🙌", "🤝", "✌️", "🤞", "🤟",
        "👋", "🖐️", "✋", "👆", "👇", "👈", "👉", "💪",
        // Hearts
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
        "💕", "💖", "💗", "💘", "💝", "💞", "💟", "❣️",
        // Symbols
        "⭐", "🌟", "✨", "💫", "🔥", "💯", "💢", "💥",
        "💦", "💨", "🎉", "🎊", "🎈", "🎁", "🏆", "🥇",
        // Objects
        "🎬", "🎥", "📹", "📷", "🎤", "🎧", "🎵", "🎶",
        "💻", "📱", "⌚", "📺", "🔔", "📢", "💡", "🔮"
    )

    companion object {
        fun newInstance(): StickersFragment {
            return StickersFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_stickers, container, false)
        
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvStickers)
        recyclerView.layoutManager = GridLayoutManager(context, 8)
        recyclerView.adapter = StickerAdapter()

        return view
    }

    inner class StickerAdapter : RecyclerView.Adapter<StickerAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvEmoji: TextView = view.findViewById(R.id.tvEmoji)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_sticker, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val emoji = emojis[position]
            holder.tvEmoji.text = emoji
            holder.itemView.setOnClickListener {
                onStickerSelected?.invoke(emoji)
                dismiss()
            }
        }

        override fun getItemCount() = emojis.size
    }
}
