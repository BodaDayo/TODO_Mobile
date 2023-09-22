package com.rgbstudios.todomobile.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rgbstudios.todomobile.databinding.ItemIconBinding
import com.rgbstudios.todomobile.utils.IconManager

class CategoryIconAdapter(
    private val iconList: List<String>,
    private val iconManager: IconManager,
    private val iconClickListener: IconClickListener
):
    RecyclerView.Adapter<CategoryIconAdapter.IconViewHolder>() {

    inner class IconViewHolder(val binding: ItemIconBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val binding =
            ItemIconBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IconViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        val icon = iconList[position]

        val iconResource = iconManager.getIconDrawableResource(icon)

        // Set the icon
        holder.binding.categoryIcon.setImageResource(iconResource)
        // Set the click  listener for categoryIconLayout
        holder.binding.categoryIconLayout.setOnClickListener {
            iconClickListener.onIconClick(icon)
        }
    }

    override fun getItemCount(): Int {
        return iconList.size
    }

    interface IconClickListener {
        fun onIconClick(iconIdentifier: String)
    }
}