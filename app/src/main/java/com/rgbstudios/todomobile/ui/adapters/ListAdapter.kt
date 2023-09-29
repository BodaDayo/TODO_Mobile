package com.rgbstudios.todomobile.ui.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.data.entity.TaskEntity
import com.rgbstudios.todomobile.databinding.ItemTaskParentBinding
import com.rgbstudios.todomobile.model.TaskList
import com.rgbstudios.todomobile.viewmodel.TodoViewModel

class ListAdapter(private val context: Context, private val viewModel: TodoViewModel) :
    RecyclerView.Adapter<ListAdapter.ListViewHolder>() {

    private var lists: List<TaskList> = emptyList()
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var binAdapter: BinAdapter

    fun updateTaskLists(newAllTasksList: List<TaskList>) {
        lists = newAllTasksList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val binding =
            ItemTaskParentBinding.inflate(LayoutInflater.from(context), parent, false)
        return ListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val list = lists[position]
        val name = list.name
        val tasks = list.list

        if (name == COMPLETED) {
            // Create and set the BinAdapter for the "completed" list
            binAdapter = BinAdapter(context, name, tasks, viewModel)
            setListData(holder, name, tasks, binAdapter)
        } else {
            // Create and set the TaskAdapter for other lists
            taskAdapter = TaskAdapter(context, name, tasks, viewModel)
            setListData(holder, name, tasks, taskAdapter)
        }
    }

    private fun setListData(holder: ListViewHolder, name: String, tasks: List<TaskEntity>, adapter: RecyclerView.Adapter<*>) {

        if (name == COMPLETED) {
            if (tasks.isNotEmpty()) {
                holder.binding.separator.visibility = View.VISIBLE
                holder.binding.listName.text = name
                holder.binding.listNameLayout.visibility = View.VISIBLE
                holder.binding.closeList.visibility = View.GONE

                // Set the icon based on visibility of the layout
                if (holder.binding.childRecyclerView.visibility == View.VISIBLE) {
                    holder.binding.collapseList.setImageResource(R.drawable.baseline_keyboard_arrow_up_24)
                } else {
                    holder.binding.collapseList.setImageResource(R.drawable.baseline_keyboard_arrow_down_24)
                }
            } else {
                holder.binding.listNameLayout.visibility = View.GONE
                holder.binding.separator.visibility = View.GONE
            }
        } else {
            if (name != UNCOMPLETED) {
                holder.binding.listName.text = name
                holder.binding.listName.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.myPrimary
                    )
                )

                holder.binding.listNameLayout.visibility = View.VISIBLE
                holder.binding.collapseList.visibility = View.GONE

                holder.binding.closeList.visibility = View.VISIBLE
            } else {
                holder.binding.listNameLayout.visibility = View.GONE
                holder.binding.separator.visibility = View.GONE
            }
        }

        holder.binding.childRecyclerView.setHasFixedSize(true)
        holder.binding.childRecyclerView.layoutManager =
            LinearLayoutManager(holder.itemView.context)
        holder.binding.childRecyclerView.adapter = adapter
    }


    fun clearTasksSelection(taskListName: String) {
        if (taskListName == COMPLETED) {
            binAdapter.clearSelection()
        } else {
            taskAdapter.clearSelection()
        }
    }

    fun fillTasKSelection(taskListName: String, list: List<TaskEntity>) {
        if (taskListName == COMPLETED) {
            binAdapter.fillSelection(list)
        } else {
            taskAdapter.fillSelection(list)
        }
    }

    override fun getItemCount(): Int {
        return lists.size
    }

    inner class ListViewHolder(val binding: ItemTaskParentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.collapseList.setOnClickListener {
                val isExpanded = binding.childRecyclerView.visibility == View.VISIBLE
                toggleListExpansion(isExpanded, binding)
            }
            binding.closeList.setOnClickListener {
                viewModel.startDatabaseListeners()
            }
        }

        private fun toggleListExpansion(isExpanded: Boolean, binding: ItemTaskParentBinding) {
            if (isExpanded) {
                binding.childRecyclerView.visibility = View.GONE
                binding.collapseList.setImageResource(R.drawable.baseline_keyboard_arrow_down_24)
            } else {
                binding.childRecyclerView.visibility = View.VISIBLE
                binding.collapseList.setImageResource(R.drawable.baseline_keyboard_arrow_up_24)
            }
            // Notify the adapter about the data set change
            notifyDataSetChanged()
        }
    }
    companion object {
        private const val UNCOMPLETED = "uncompleted"
        private const val COMPLETED = "completed"
    }

}