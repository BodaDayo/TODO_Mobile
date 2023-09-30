package com.rgbstudios.todomobile.ui.adapters

import android.app.UiModeManager
import android.content.Context
import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.data.entity.TaskEntity
import com.rgbstudios.todomobile.databinding.ItemTaskBinding
import com.rgbstudios.todomobile.viewmodel.TodoViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskAdapter(
    private val context: Context,
    private val name: String,
    private val tasks: List<TaskEntity>,
    private val viewModel: TodoViewModel
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private var highlightedListName: String = "" // Keep track of list currently being selected from
    private val selectedItems = mutableListOf<Int>() // Keep track of selected items
    private var isInSelectMode = false // Keep track of select mode

    init {
        // Update highlightedListName from its storage in the viewModel
        viewModel.highlightedListName.observeForever {
            highlightedListName = it
        }
    }

    inner class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        val isSelected = isItemSelected(position)

        var newTaskCompleted = task.taskCompleted
        var newStarred = task.starred
        var dueDateTime = task.dueDateTime
        val taskCategories = task.categoryIds

        val uiModeManager =
            holder.itemView.context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

        holder.binding.apply {

            taskLayout.setOnLongClickListener {
                if (!isInSelectMode && highlightedListName == "") {
                    toggleSelection(position, task)
                }
                true // Consume the long-click event
            }

            markCompletedImageView.setOnLongClickListener {
                if (!isInSelectMode && highlightedListName == "") {
                    toggleSelection(position, task)
                }
                true // Consume the long-click event
            }

            taskDetailsLayout.setOnLongClickListener {
                if (!isInSelectMode && highlightedListName == "") {
                    toggleSelection(position, task)
                }
                true // Consume the long-click event
            }

            star.setOnLongClickListener {
                if (!isInSelectMode && highlightedListName == "") {
                    toggleSelection(position, task)
                }
                true // Consume the long-click event
            }

            taskDetailsLayout.setOnClickListener {
                if (isInSelectMode && highlightedListName == name) {
                    // If in select mode, toggle selection on click
                    toggleSelection(position, task)
                } else if (highlightedListName == "") {

                    // Set the selected task data in the ViewModel
                    viewModel.setSelectedTaskData(task)

                    // Navigate to the EditTaskFragment using the Navigation Component
                    root.findNavController()
                        .navigate(R.id.action_homeFragment_to_editTaskFragment)
                }
            }

            star.setOnClickListener {
                if (isInSelectMode && highlightedListName == name) {
                    // If in select mode, toggle selection on click
                    toggleSelection(position, task)
                } else if (highlightedListName == "") {

                    newStarred = !newStarred

                    updateTaskData(
                        task.taskId,
                        task.title,
                        task.description,
                        newTaskCompleted,
                        newStarred,
                        dueDateTime,
                        taskCategories
                    )
                }
            }

            markCompletedImageView.setOnClickListener {
                if (isInSelectMode && highlightedListName == name) {
                    // If in select mode, toggle selection on click
                    toggleSelection(position, task)
                } else if (highlightedListName == "") {

                    newTaskCompleted = !newTaskCompleted
                    if (newTaskCompleted) {
                        dueDateTime = null
                    }

                    // TODO markCompleted animation or not

                    updateTaskData(
                        task.taskId,
                        task.title,
                        task.description,
                        newTaskCompleted,
                        newStarred,
                        dueDateTime,
                        taskCategories
                    )
                }
            }

            taskTitleTextView.text = task.title
            taskDescriptionTextView.text = task.description

            if (dueDateTime != null) {
                val formattedDateTime = formatDateTime(dueDateTime!!)

                // Check if dueDateTime is in the past
                val currentTime = Calendar.getInstance()
                if (dueDateTime!!.before(currentTime)) {
                    // Set the text color to R.color.poor
                    taskDateTime.setTextColor(ContextCompat.getColor(context, R.color.poor))
                    val overdueTime = formattedDateTime + context.getString(R.string.overdue_task)
                    taskDateTime.text = overdueTime
                } else {
                    // Set the text color to the default color
                    taskDateTime.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.my_darker_grey
                        )
                    )
                    taskDateTime.text = formattedDateTime
                }
                taskDateTime.visibility = View.VISIBLE
            } else {
                taskDateTime.visibility = View.GONE
            }

            taskTitleTextView.text = task.title
            taskDescriptionTextView.text = task.description

            // Check if the task is completed, and apply the strikethrough effect and change icon if true
            if (task.taskCompleted) {
                taskTitleTextView.paintFlags =
                    taskTitleTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                taskTitleTextView.setTextColor(
                    ContextCompat.getColor(
                        root.context,
                        com.google.android.material.R.color.material_on_surface_disabled
                    )
                )
                taskDateTime.visibility = View.GONE
                taskDescriptionTextView.visibility = View.GONE
                markCompletedImageView.setImageResource(R.drawable.checkboxd)
                markCompletedImageView.setColorFilter(
                    ContextCompat.getColor(
                        root.context,
                        com.google.android.material.R.color.design_default_color_primary
                    )
                )
                star.visibility = View.GONE
            } else {
                taskTitleTextView.paintFlags =
                    taskTitleTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                markCompletedImageView.setImageResource(R.drawable.check)
            }

            // Check if newStarred, and change icon if true
            val starIcon = if (newStarred) R.drawable.star_filled else R.drawable.star
            star.setImageResource(starIcon)

            if (taskDescriptionTextView.text.isEmpty()) {
                taskDescriptionTextView.visibility = View.GONE
            }

            // Get the background
            val drawableResourceId = if (isNightMode) {
                R.drawable.highlight_rectangle_background_night // Use night mode color resource
            } else {
                R.drawable.highlight_rectangle_background // Use regular color resource
            }

            // Get the taskDateTime color
            val colorResourceId = if (isNightMode) {
                R.color.myOnSurfaceNight // Use night mode color resource
            } else {
                R.color.myOnSurfaceDay // Use regular color resource
            }

            val textColorSelected = ContextCompat.getColor(holder.itemView.context, colorResourceId)
            val textColorDefault = ContextCompat.getColor(holder.itemView.context, R.color.my_darker_grey)

            // Update the UI based on selection state
            if (isSelected) {
                // Highlight the view
                taskLayout.setBackgroundResource(drawableResourceId)
                selectTaskCheckView.visibility = View.VISIBLE
                markCompletedImageView.visibility = View.GONE
                taskDateTime.setTextColor(textColorSelected)
            } else {
                // Deselect the view
                taskLayout.setBackgroundResource(R.drawable.transparent_rectangle_background)
                selectTaskCheckView.visibility = View.GONE
                markCompletedImageView.visibility = View.VISIBLE
                taskDateTime.setTextColor(textColorDefault)
            }
        }
    }

    // ---------------------------------------------------------------------


    // Check if an item is selected
    private fun isItemSelected(position: Int): Boolean {
        return selectedItems.contains(position)
    }

    // Toggle selection of an item
    private fun toggleSelection(position: Int, taskEntity: TaskEntity) {

        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }

        viewModel.toggleSelection(taskEntity)

        // Set select mode state and update UI accordingly
        isInSelectMode = selectedItems.isNotEmpty()

        val newName = if (isInSelectMode) name else ""
        viewModel.updateHighlightedListName(newName)

        notifyDataSetChanged()
    }

    // Deselect all items
    fun clearSelection() {
        selectedItems.clear()

        viewModel.clearSelection()

        // Set select mode state and update UI accordingly
        isInSelectMode = selectedItems.isNotEmpty()

        val newName = ""
        viewModel.updateHighlightedListName(newName)

        viewModel.setIsSelectionModeOn(isInSelectMode)
        notifyDataSetChanged()
    }

    fun fillSelection(list:  List<TaskEntity>) {
        selectedItems.clear()

        val allItems = list.indices.toList()
        selectedItems.addAll(allItems)

        viewModel.fillSelection(list)

        // Set select mode state and update UI accordingly
        isInSelectMode = selectedItems.isNotEmpty()

        val newName = if (isInSelectMode) name else ""
        viewModel.updateHighlightedListName(newName)

        viewModel.setIsSelectionModeOn(isInSelectMode)
        notifyDataSetChanged()
    }
    // -------------------------------------------------------------------------------

    override fun getItemCount(): Int {
        return tasks.size
    }

    private fun updateTaskData(
        id: String,
        title: String,
        description: String,
        completed: Boolean,
        starred: Boolean,
        dueDateTime: Calendar?,
        taskCategories: List<String>
    ) {

        // Call the ViewModel's method to update the task
        viewModel.updateTask(
            id,
            title,
            description,
            completed,
            starred,
            dueDateTime,
            taskCategories
        ) { isSuccessful ->
            if (isSuccessful) {
                return@updateTask
            }
        }

    }

    private fun formatDateTime(dateTime: Calendar): String {
        val now = Calendar.getInstance()
        val sdf = SimpleDateFormat("EEE, MMM dd, yyyy 'At' hh:mm a", Locale.getDefault())

        val isToday = now.get(Calendar.YEAR) == dateTime.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == dateTime.get(Calendar.DAY_OF_YEAR)

        if (isToday) {
            sdf.applyPattern("'Today At' hh:mm a")
        } else {
            val tomorrow = Calendar.getInstance()
            tomorrow.add(Calendar.DAY_OF_YEAR, 1)
            if (tomorrow.get(Calendar.YEAR) == dateTime.get(Calendar.YEAR) &&
                tomorrow.get(Calendar.DAY_OF_YEAR) == dateTime.get(Calendar.DAY_OF_YEAR)
            ) {
                sdf.applyPattern("'Tomorrow At' hh:mm a")
            }
        }

        return sdf.format(dateTime.time)
    }
}