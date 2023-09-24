package com.rgbstudios.todomobile.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rgbstudios.todomobile.databinding.ItemEmojiBinding

class EmojiAdapter(
    private val emojiList: List<Triple<String, Int, Int>>,
    private val emojiClickListener: EmojiClickListener
) :
    RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder>() {
    private var selectedColorPosition = -1

    inner class EmojiViewHolder(val binding: ItemEmojiBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val binding =
            ItemEmojiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EmojiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        val emojiTriple = emojiList[position]

        val emojiResource = emojiTriple.second

        // Set the emoji
        holder.binding.emojiImageView.setImageResource(emojiResource)

        // Set the click  listener for emojiItemLayout
        holder.binding.emojiItemLayout.setOnClickListener {
            emojiClickListener.onEmojiClick(emojiTriple)
            selectItem(position)
        }

        if (position == selectedColorPosition) {
            holder.binding.emojiSelectedView.visibility = View.VISIBLE
        } else {
            holder.binding.emojiSelectedView.visibility = View.INVISIBLE
        }
    }

    private fun selectItem(position: Int) {
        val previousSelectedColorPosition = selectedColorPosition
        selectedColorPosition = position
        notifyItemChanged(previousSelectedColorPosition)
        notifyItemChanged(selectedColorPosition)
    }

    override fun getItemCount(): Int {
        return emojiList.size
    }

    interface EmojiClickListener {
        fun onEmojiClick(emojiTriple: Triple<String, Int, Int>)
    }

}