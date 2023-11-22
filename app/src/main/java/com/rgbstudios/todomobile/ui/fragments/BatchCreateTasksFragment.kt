package com.rgbstudios.todomobile.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.databinding.FragmentBatchCreateTasksBinding
import com.rgbstudios.todomobile.ui.adapters.BatchTaskAdapter
import com.rgbstudios.todomobile.utils.DialogManager
import com.rgbstudios.todomobile.utils.ToastManager
import com.rgbstudios.todomobile.viewmodel.TodoViewModel

class BatchCreateTasksFragment : Fragment() {

    private val sharedViewModel: TodoViewModel by activityViewModels()
    private lateinit var binding: FragmentBatchCreateTasksBinding
    private lateinit var fragmentContext: Context
    private lateinit var batchTaskAdapter: BatchTaskAdapter
    private val toastManager = ToastManager()
    private val dialogManager = DialogManager()
    private val batchList = getDefaultList()
    private val thisFragment = this

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBatchCreateTasksBinding.inflate(inflater, container, false)
        fragmentContext = requireContext()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            batchTaskAdapter = BatchTaskAdapter(thisFragment, batchList, dialogManager, batchTasksRecyclerView)

            //batchTasksRecyclerView.setHasFixedSize(true)
            batchTasksRecyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            batchTasksRecyclerView.adapter = batchTaskAdapter

            fab.setOnClickListener {
                val batchListSize = batchTaskAdapter.itemCount

                if (batchListSize >= 10 ) {
                    val errorMessage = getString(R.string.batch_task_limit_exceeded_message)
                    toastManager.showShortToast(requireContext(), errorMessage)
                } else {
                    batchTaskAdapter.addTask()
                }
            }

            saveButton.setOnClickListener {
                saveBatchTasks()
            }

            popBack.setOnClickListener {
                popBackStackManager()
            }

            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                popBackStackManager()
            }
        }
    }

    private fun saveBatchTasks() {
        val tasks = batchTaskAdapter.getAllTasks()
        if (tasks != null) {
            sharedViewModel.saveMultipleTask(tasks) { isSuccessful ->
                if (isSuccessful) {
                    // Handle success
                    toastManager.showShortToast(
                        fragmentContext,
                        "Tasks saved successfully!"
                    )
                } else {
                    // Handle failure
                    toastManager.showShortToast(
                        fragmentContext,
                        "Failed to save task"
                    )
                }

                popBackStackManager()
            }
        } else {
            toastManager.showShortToast(requireContext(), "Ensure all tasks have a title")
        }
    }

    private fun getDefaultList(): MutableList<Int> {
        val taskList = mutableListOf<Int>()

        for (i in 1..3) {
            taskList.add(i)
        }
        return taskList
    }

    private fun popBackStackManager() {
        if (batchTaskAdapter.areTasksFilled()) {
            dialogManager.showDiscardDialog(this) { isSuccessful ->
                if (isSuccessful) {
                    // pop back stack
                    activity?.supportFragmentManager?.popBackStack()
                }
            }
        } else {
            // If no changes, simply pop the back stack
            activity?.supportFragmentManager?.popBackStack()
        }
    }
}