package com.rgbstudios.todomobile.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
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

@SuppressLint("NotifyDataSetChanged")
class ListAdapter(private val context: Context, private val viewModel: TodoViewModel) :
    RecyclerView.Adapter<ListAdapter.ListViewHolder>() {

    private var lists: List<TaskList> = emptyList()
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var binAdapter: TaskAdapter

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
            binAdapter = TaskAdapter(context, name, tasks, viewModel)
            setListData(holder, name, tasks, binAdapter)
        } else {
            // Create and set the TaskAdapter for other lists
            taskAdapter = TaskAdapter(context, name, tasks, viewModel)
            setListData(holder, name, tasks, taskAdapter)
        }
    }

    private fun setListData(
        holder: ListViewHolder,
        name: String,
        tasks: List<TaskEntity>,
        adapter: RecyclerView.Adapter<*>
    ) {
        holder.binding.apply {
            if (name == COMPLETED) {
                handleCompletedList(holder, name, tasks)
            } else {
                handleUncompletedList(holder, name)
            }

            childRecyclerView.setHasFixedSize(true)
            childRecyclerView.layoutManager =
                LinearLayoutManager(holder.itemView.context)
            childRecyclerView.adapter = adapter
        }
    }

    private fun handleCompletedList(
        holder: ListViewHolder,
        name: String,
        tasks: List<TaskEntity>
    ) {
        holder.binding.apply {
            if (tasks.isNotEmpty()) {
                separator.visibility = View.VISIBLE
                listName.text = name
                listNameLayout.visibility = View.VISIBLE
                closeList.visibility = View.GONE

                // Set the icon based on visibility of the layout
                if (childRecyclerView.visibility == View.VISIBLE) {
                    collapseList.setImageResource(R.drawable.arrow_up)
                } else {
                    collapseList.setImageResource(R.drawable.arrow_down)
                }
                listName.isEnabled = true
            } else {
                listNameLayout.visibility = View.GONE
                separator.visibility = View.GONE
            }
        }
    }

    private fun handleUncompletedList(
        holder: ListViewHolder,
        name: String,
    ) {
        holder.binding.apply {
            if (name != UNCOMPLETED) {
                listName.text = name
                listName.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.myPrimary
                    )
                )

                listNameLayout.visibility = View.VISIBLE
                collapseList.visibility = View.GONE

                closeList.visibility = View.VISIBLE
            } else {
                listNameLayout.visibility = View.GONE
                separator.visibility = View.GONE
            }
            listName.isEnabled = false
        }
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
            binding.listName.setOnClickListener {
                val isExpanded = binding.childRecyclerView.visibility == View.VISIBLE
                toggleListExpansion(isExpanded, binding)
            }
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
                binding.collapseList.setImageResource(R.drawable.arrow_down)
            } else {
                binding.childRecyclerView.visibility = View.VISIBLE
                binding.collapseList.setImageResource(R.drawable.arrow_up)
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