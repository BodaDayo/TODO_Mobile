package com.rgbstudios.todomobile.ui.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.databinding.ItemColorBinding
import com.rgbstudios.todomobile.utils.ColorManager

class CategoryColorAdapter(
    private val colorList: List<String>,
    private val colorManager: ColorManager,
    private val colorClickListener: ColorClickListener
):
    RecyclerView.Adapter<CategoryColorAdapter.ColorViewHolder>() {

    private var selectedColorPosition = -1

    inner class ColorViewHolder(val binding: ItemColorBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val binding =
            ItemColorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ColorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val color = colorList[position]

        val colorPair = colorManager.getColorMapping(color)

        // Set the background for categoryColorView
        holder.binding.categoryColorView.setBackgroundResource(R.drawable.circular_primary_background)

        // Set the background tint for categoryColorView
        holder.binding.categoryColorView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.context, colorPair.first))

        // Set the click  listener for categoryColorView
        holder.binding.categoryColorView.setOnClickListener {
            colorClickListener.onColorClick(color)
            selectItem(position)
        }
        if (position == selectedColorPosition) {
            holder.binding.categoryColorBack.visibility = View.VISIBLE
        } else {
            holder.binding.categoryColorBack.visibility = View.INVISIBLE
        }
    }

    private fun selectItem(position: Int) {
        val previousSelectedColorPosition = selectedColorPosition
        selectedColorPosition = position
        notifyItemChanged(previousSelectedColorPosition)
        notifyItemChanged(selectedColorPosition)
    }

    override fun getItemCount(): Int {
        return colorList.size
    }

    interface ColorClickListener {
        fun onColorClick(colorIdentifier: String)
    }
}