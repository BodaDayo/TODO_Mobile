package com.rgbstudios.todomobile.ui.adapters

import android.app.UiModeManager
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.data.entity.CategoryEntity
import com.rgbstudios.todomobile.databinding.ItemCategoryBarBinding
import com.rgbstudios.todomobile.utils.ColorManager
import com.rgbstudios.todomobile.utils.IconManager
import com.rgbstudios.todomobile.viewmodel.TodoViewModel

class EditFragmentCategoryAdapter(
    private val context: Context,
    private val viewModel: TodoViewModel
) :
    RecyclerView.Adapter<EditFragmentCategoryAdapter.CategoryViewHolder>() {

    private var categoriesList: List<CategoryEntity> = emptyList()
    private val colorManager = ColorManager()
    private val iconManager = IconManager()

    fun updateTaskLists(newCategoriesList: List<CategoryEntity>) {
        categoriesList = newCategoriesList
        notifyDataSetChanged()
    }

    inner class CategoryViewHolder(val binding: ItemCategoryBarBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding =
            ItemCategoryBarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categoriesList[position]

        val icon = category.categoryIconIdentifier
        val iconResource = iconManager.getIconDrawableResource(icon)

        val color = category.categoryColorIdentifier
        val colorPair = colorManager.getColorMapping(color)

        val uiModeManager =
            holder.itemView.context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

        holder.binding.apply {

            // Get the stroke color
            val colorResourceId = if (isNightMode) {
                colorPair.first // Use night mode color resource
            } else {
                colorPair.second // Use regular color resource
            }

            // Get the background drawable and cast it to GradientDrawable
            val backgroundDrawable = ContextCompat.getDrawable(
                holder.itemView.context,
                R.drawable.rounded_corners_tag
            ) as GradientDrawable

            val borderColor = ContextCompat.getColor(holder.itemView.context, colorResourceId)

            // Set the stroke color to colorResourceId
            backgroundDrawable.setStroke(2, borderColor)

            // Set the modified background drawable as the background
            categoryBarItemBackground.background = backgroundDrawable

            // Set the icon tint of categoryImageView
            categoryBarImageView.setColorFilter(
                ContextCompat.getColor(holder.itemView.context, colorPair.second),
                PorterDuff.Mode.SRC_IN
            )
            // Set the category icon
            categoryBarImageView.setImageResource(iconResource)

            // Set the category name
            categoryBarNameTextView.text = category.categoryName

            // Set the text color of categoryBarNameTextView to colorResourceId
            categoryBarNameTextView.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    colorResourceId
                )
            )

            categoryBarItemBackground.setOnClickListener {
                viewModel.removeTaskFromCategory(category) { isSuccessful ->
                    if (isSuccessful) {
                        // Handle success
                        Toast.makeText(
                            context,
                            "Task removed from ${category.categoryName} Category",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    } else {
                        // Handle failure
                        Toast.makeText(
                            context,
                            "Failed to remove task from ${category.categoryName} category",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            }
        }

    }

    override fun getItemCount(): Int {
        return categoriesList.size
    }
}