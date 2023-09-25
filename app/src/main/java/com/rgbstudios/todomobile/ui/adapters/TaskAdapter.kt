package com.rgbstudios.todomobile.ui.adapters

import android.content.Context
import android.graphics.Paint
import android.text.format.DateUtils.formatDateTime
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.data.entity.CategoryEntity
import com.rgbstudios.todomobile.data.entity.TaskEntity
import com.rgbstudios.todomobile.databinding.ItemTaskBinding
import com.rgbstudios.todomobile.viewmodel.TodoViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskAdapter(
    private val context: Context,
    private val tasks: List<TaskEntity>,
    private val viewModel: TodoViewModel
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.binding.taskTitleTextView.text = task.title
        holder.binding.taskDescriptionTextView.text = task.description

        var newTaskCompleted = task.taskCompleted
        var newStarred = task.starred
        var dueDateTime = task.dueDateTime
        val taskCategories = task.categoryIds

        holder.binding.apply {

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
                    taskDateTime.setTextColor(ContextCompat.getColor(context, R.color.my_darker_grey))
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

            taskDetailsLayout.setOnClickListener {
                // Set the selected task data in the ViewModel
                viewModel.setSelectedTaskData(task)

                // Navigate to the EditTaskFragment using the Navigation Component
                root.findNavController()
                    .navigate(R.id.action_homeFragment_to_editTaskFragment)
            }

            star.setOnClickListener {
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

            markCompletedImageView.setOnClickListener {
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
    }

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