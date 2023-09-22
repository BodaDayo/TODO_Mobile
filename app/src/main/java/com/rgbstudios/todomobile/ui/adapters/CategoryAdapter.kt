package com.rgbstudios.todomobile.ui.adapters

import android.app.Dialog
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.data.entity.CategoryEntity
import com.rgbstudios.todomobile.databinding.DialogCategorySelectionBinding
import com.rgbstudios.todomobile.databinding.ItemCategoryBinding
import com.rgbstudios.todomobile.utils.ColorManager
import com.rgbstudios.todomobile.utils.IconManager

class CategoryAdapter(
    private val iconManager: IconManager,
    private val colorManager: ColorManager,
    private val dialog: Dialog,
    private val dialogBinding: DialogCategorySelectionBinding,
    private val categoryClickListener: CategoryClickListener,
) :
    RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var selectedColorPosition = -1

    private var categoryList: List<CategoryEntity> = emptyList()

    fun updateCategoryList(newCategoryList: List<CategoryEntity>) {
        selectedColorPosition = -1
        categoryList = newCategoryList
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categoryList[position]

        val icon = category.categoryIconIdentifier
        val iconResource = iconManager.getIconDrawableResource(icon)

        val color = category.categoryColorIdentifier
        val colorPair = colorManager.getColorMapping(color)

        holder.binding.apply {

            // Set the background color of categoryImageView
            categoryImageView.setBackgroundResource(colorPair.first)

            // Set the icon tint of categoryImageView
            categoryImageView.setColorFilter(
                ContextCompat.getColor(holder.itemView.context, colorPair.second),
                PorterDuff.Mode.SRC_IN
            )

            // Set the category icon
            categoryImageView.setImageResource(iconResource)

            // Set the category name
            categoryNameTextView.text = category.categoryName

            // Set click listener for the categoryImageView
            categoryImageView.setOnClickListener {
                categoryClickListener.onCategoryClick(category, dialog)
            }

            categoryImageView.setOnLongClickListener {
                categoryClickListener.onCategoryLongClick(category, dialog, dialogBinding)
                selectItem(position)
                true
            }

            if (position == selectedColorPosition) {
                holder.binding.selectedIcon.visibility = View.VISIBLE
                categoryNameTextView.setBackgroundResource(R.color.myPrimaryVariant)
                categoryImageLayout.setBackgroundResource(R.color.myPrimaryVariant)
            } else {
                holder.binding.selectedIcon.visibility = View.INVISIBLE
                categoryNameTextView.setBackgroundResource(android.R.color.transparent)
                categoryImageLayout.setBackgroundResource(colorPair.first)
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
        return categoryList.size
    }

    interface CategoryClickListener {
        fun onCategoryClick(category: CategoryEntity, dialog: Dialog)
        fun onCategoryLongClick(category: CategoryEntity, dialog: Dialog, dialogBinding: DialogCategorySelectionBinding)
    }
}
