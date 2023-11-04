package com.rgbstudios.todomobile.ui.adapters

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.utilities.MaterialDynamicColors.background
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.databinding.ItemThemeBinding
import com.rgbstudios.todomobile.utils.ColorManager

class AppThemeAdapter(
    private val colorList: List<String>,
    private val colorManager: ColorManager
) :
    RecyclerView.Adapter<AppThemeAdapter.ColorViewHolder>() {

    private var selectedColorPosition = -1

    inner class ColorViewHolder(val binding: ItemThemeBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val binding =
            ItemThemeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ColorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val color = colorList[position]

        val colorPair = colorManager.getColorMapping(color)

        holder.binding.apply {
            themeLightColor.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    holder.itemView.context,
                    colorPair.first
                )
            )
            themeDarkColor.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    holder.itemView.context,
                    colorPair.second
                )
            )

            itemThemeLayout.setOnClickListener {
                selectItem(position)
                //Todo set the theme in sharedPrefs
            }

            // Get the background drawable and cast it to GradientDrawable
            val backgroundDrawable = ContextCompat.getDrawable(
                holder.itemView.context,
                R.drawable.rounded_corners_theme
            ) as GradientDrawable


            if (position == selectedColorPosition) {
                themeOverlay.visibility = View.VISIBLE
                selectedMark.visibility = View.VISIBLE

                val borderColor = ContextCompat.getColor(holder.itemView.context, R.color.myPrimary)

                // Set the stroke color to colorResourceId
                backgroundDrawable.setStroke(6, borderColor)

                // Set the modified background drawable as the background
                itemThemeLayout.background = backgroundDrawable

            } else {
                themeOverlay.visibility = View.INVISIBLE
                selectedMark.visibility = View.INVISIBLE

                val borderColor = ContextCompat.getColor(holder.itemView.context, R.color.my_darker_grey)

                // Set the stroke color to colorResourceId
                backgroundDrawable.setStroke(6, borderColor)

                // Set the modified background drawable as the background
                itemThemeLayout.background = backgroundDrawable
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
        return colorList.size
    }
}