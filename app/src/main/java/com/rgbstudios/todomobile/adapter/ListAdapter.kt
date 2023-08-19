package com.rgbstudios.todomobile.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.databinding.ItemTaskBinding
import com.rgbstudios.todomobile.databinding.ItemTaskParentBinding
import com.rgbstudios.todomobile.model.TaskData
import com.rgbstudios.todomobile.model.TaskList
import com.rgbstudios.todomobile.model.TaskViewModel

class ListAdapter(private val viewModel: TaskViewModel) :
    RecyclerView.Adapter<ListAdapter.ListViewHolder>() {

    private var lists: List<TaskList> = emptyList()

    fun updateTaskLists(newAllTasksList: List<TaskList>) {
        lists = newAllTasksList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val binding =
            ItemTaskParentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val list = lists[position]
        val taskAdapter = TaskAdapter(list.list, viewModel)

        if (list.name == "completed") {
            holder.binding.separator.visibility = View.VISIBLE
            holder.binding.listName.text = list.name
            holder.binding.listNameLayout.visibility = View.VISIBLE

            // Set the initial icon based on visibility
            if (holder.binding.childRecyclerView.visibility == View.VISIBLE) {
                holder.binding.collapseList.setImageResource(R.drawable.baseline_keyboard_arrow_up_24)
            } else {
                holder.binding.collapseList.setImageResource(R.drawable.baseline_keyboard_arrow_down_24)
            }
        } else {
            // Hide the listNameLayout and separator for uncompleted lists
            holder.binding.separator.visibility = View.GONE
            holder.binding.listNameLayout.visibility = View.GONE
        }

        holder.binding.childRecyclerView.setHasFixedSize(true)
        holder.binding.childRecyclerView.layoutManager =
            LinearLayoutManager(holder.itemView.context)
        holder.binding.childRecyclerView.adapter = taskAdapter

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
}