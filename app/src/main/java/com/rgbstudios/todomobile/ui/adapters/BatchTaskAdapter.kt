package com.rgbstudios.todomobile.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.data.entity.TaskEntity
import com.rgbstudios.todomobile.databinding.ItemCreateTaskBinding
import com.rgbstudios.todomobile.utils.DialogManager
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class BatchTaskAdapter(
    private val fragment: Fragment,
    private var batchList: MutableList<Int>,
    private val dialogManager: DialogManager,
    private val recyclerView: RecyclerView
) : RecyclerView.Adapter<BatchTaskAdapter.ViewHolder>() {

    private val context = fragment.context

    inner class ViewHolder(val binding: ItemCreateTaskBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemCreateTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.binding.apply {

            batchTaskDateTimeTVB.setOnClickListener {
                showDatePickerDialog { formattedDateTime ->
                    batchTaskDateTimeTVB.text = formattedDateTime
                }
            }

            removeTask.setOnClickListener {
                removeTask(holder.adapterPosition)
            }
        }
    }

    override fun getItemCount(): Int {
        return this.batchList.size
    }

    fun getAllTasks(): List<TaskEntity>? {

        val newList = mutableListOf<TaskEntity>()

        for (position in 0 until itemCount) {
            val holder = recyclerView.findViewHolderForAdapterPosition(position) as ViewHolder

            val taskId = UUID.randomUUID().toString()

            // Get information from the views in the ViewHolder
            val title = holder.binding.batchTaskTitleEt.text.toString()
            val description = holder.binding.batchTaskDescriptionEt.text.toString()

            val defaultDateString = context?.getString(R.string.due_date)

            // Check if batchTaskDateTimeTVB has a non-default text value
            val dueDateTimeText = holder.binding.batchTaskDateTimeTVB.text.toString()
            val dueDateTime =
                if (dueDateTimeText != defaultDateString) {
                    // Parse the formatted date and time back to Calendar
                    parseFormattedDateTime(dueDateTimeText)
                } else {
                    null
                }

            if (title.isBlank()) return null

            // Create a new TaskEntity with the updated information
            val taskEntity = TaskEntity(
                taskId = taskId,
                title = title,
                description = description,
                taskCompleted = false,
                starred = false,
                dueDateTime = dueDateTime
            )
            // Add the new TaskEntity to the newList
            newList.add(taskEntity)
        }
        return newList
    }

    fun areTasksFilled(): Boolean {
        for (position in 0 until itemCount) {
            val holder = recyclerView.findViewHolderForAdapterPosition(position) as ViewHolder?
            if (holder != null) {
                val title = holder.binding.batchTaskTitleEt.text.toString()
                val description = holder.binding.batchTaskDescriptionEt.text.toString()
                val dueDateTimeText = holder.binding.batchTaskDateTimeTVB.text.toString()
                val defaultDateString = context?.getString(R.string.due_date)

                // Check if title or description is not blank, or dueDateTimeText is not the default value
                if (title.isNotBlank() || description.isNotBlank() || dueDateTimeText != defaultDateString) {
                    return true // At least one task is filled
                }
            }
        }

        return false
    }

    fun addTask() {
        val newNumber = batchList.size + 1
        batchList.add(newNumber)
        notifyItemInserted(batchList.size - 1)
    }

    private fun removeTask(position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            val holder = recyclerView.findViewHolderForAdapterPosition(position) as? ViewHolder
            holder?.binding?.batchTaskTitleEt?.text?.clear()
            holder?.binding?.batchTaskDescriptionEt?.text?.clear()

            batchList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    private fun showDatePickerDialog(callback: (String) -> Unit) {
        dialogManager.showDatePickerDialog(fragment, null) { selectedDate ->
            if (selectedDate != null) {
                dialogManager.showTimePickerDialog(fragment, null, selectedDate) { selectedTime ->
                    if (selectedTime != null) {
                        val formattedDateTime = formatDateTime(selectedDate, selectedTime)
                        callback.invoke(formattedDateTime)
                    }
                }
            }
        }
    }

    private fun formatDateTime(date: Calendar, time: Calendar): String {
        val dateTime = Calendar.getInstance().apply {
            set(
                date.get(Calendar.YEAR),
                date.get(Calendar.MONTH),
                date.get(Calendar.DAY_OF_MONTH),
                time.get(Calendar.HOUR_OF_DAY),
                time.get(Calendar.MINUTE)
            )
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        return sdf.format(dateTime.time)
    }

    private fun parseFormattedDateTime(formattedDateTime: String): Calendar {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val parsedDate = Calendar.getInstance()

        try {
            parsedDate.time = sdf.parse(formattedDateTime) ?: Date()
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        return parsedDate
    }
}