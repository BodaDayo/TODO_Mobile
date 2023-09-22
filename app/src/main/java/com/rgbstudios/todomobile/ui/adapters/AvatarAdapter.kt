package com.rgbstudios.todomobile.ui.adapters

import android.app.Dialog
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rgbstudios.todomobile.databinding.ItemAvatarBinding

class AvatarAdapter(
    private var avatarList: List<Int>,
    private val avatarClickListener: AvatarClickListener,
) :
    RecyclerView.Adapter<AvatarAdapter.ViewHolder>() {

    private var selectedColorPosition = -1

    inner class ViewHolder(val binding: ItemAvatarBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemAvatarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val avatarImageResource = avatarList[position]


        holder.binding.apply {

            // Set the avatar image
            defaultAvatarImageView.setImageResource(avatarImageResource)

            defaultAvatarLayout.setOnClickListener {
                avatarClickListener.onAvatarClick(avatarImageResource)
                selectItem(position)
            }

            if (position == selectedColorPosition) {
                holder.binding.defaultAvatarBack.visibility = View.VISIBLE
            } else {
                holder.binding.defaultAvatarBack.visibility = View.INVISIBLE
            }
        }
    }

    private fun selectItem(position: Int) {
        val previousSelectedColorPosition = selectedColorPosition
        selectedColorPosition = position
        notifyItemChanged(previousSelectedColorPosition)
        notifyItemChanged(selectedColorPosition)
    }

    override fun getItemCount(): Int {
        return avatarList.size
    }

    interface AvatarClickListener {
        fun onAvatarClick(avatar: Int)

    }
}