package com.rgbstudios.todomobile.ui.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.data.entity.CategoryEntity
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import com.rgbstudios.todomobile.databinding.FragmentEditTaskBinding
import com.rgbstudios.todomobile.ui.adapters.EditFragmentCategoryAdapter
import com.rgbstudios.todomobile.utils.DialogManager
import com.rgbstudios.todomobile.utils.ToastManager
import com.rgbstudios.todomobile.viewmodel.TodoViewModel

class EditTaskFragment : Fragment() {

    private val sharedViewModel: TodoViewModel by activityViewModels()
    private lateinit var binding: FragmentEditTaskBinding
    private lateinit var fragmentContext: Context
    private lateinit var editFragmentCategoryAdapter: EditFragmentCategoryAdapter
    private var firstCategoryObservation = true
    private var changesMade = false // To track changes to task details
    private val dialogManager = DialogManager()
    private val toastManager = ToastManager()
    private val firebase = FirebaseAccess()

    // Variables to store data from selected data
    private var taskId = ""
    private var taskCompleted = false
    private var newStarred = false
    private var newSelectedTaskCategories: List<CategoryEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditTaskBinding.inflate(inflater, container, false)
        fragmentContext = requireContext()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve the selected task data from the ViewModel directly
        val selectedTaskData = sharedViewModel.selectedTaskData.value

        if (selectedTaskData != null) {

            // Get data from selected data
            taskId = selectedTaskData.taskId
            taskCompleted = selectedTaskData.taskCompleted
            newStarred = selectedTaskData.starred

            // Update the UI with the selected task data
            binding.editTitleEt.text =
                Editable.Factory.getInstance().newEditable(selectedTaskData.title)

            binding.editDescriptionEt.text =
                Editable.Factory.getInstance().newEditable(selectedTaskData.description)

            updateStarIcon()

            editFragmentCategoryAdapter = EditFragmentCategoryAdapter(
                fragmentContext,
                sharedViewModel
            )
            binding.tagRecyclerView.layoutManager =
                LinearLayoutManager(fragmentContext, LinearLayoutManager.HORIZONTAL, false)
            binding.tagRecyclerView.adapter = editFragmentCategoryAdapter

            sharedViewModel.selectedTaskCategories.observe(viewLifecycleOwner) { selectedTaskCategories ->
                if (selectedTaskCategories.isNotEmpty()) {
                    newSelectedTaskCategories = selectedTaskCategories

                    editFragmentCategoryAdapter.updateTaskLists(selectedTaskCategories)

                    // Refresh the categoriesList
                    sharedViewModel.startDatabaseListeners()

                    binding.tagRecyclerView.visibility = View.VISIBLE

                    changesMade = if (firstCategoryObservation) {
                        firstCategoryObservation = false
                       false
                    } else {
                        true
                    }
                    } else {
                    binding.tagRecyclerView.visibility = View.GONE
                }
            }

            // Check if the task is completed, change icon if true
            if (selectedTaskData.taskCompleted) {
                binding.markCompletedIcon.setImageResource(R.drawable.checkboxd)
                binding.markCompletedIcon.setColorFilter(
                    ContextCompat.getColor(
                        fragmentContext,
                        com.google.android.material.R.color.design_default_color_primary
                    )
                )
            } else {
                binding.markCompletedIcon.setImageResource(R.drawable.check)
            }

            // set onClick listener for star button
            binding.star.setOnClickListener {
                newStarred = !newStarred // Toggle the starred status

                // Change the star icon based on the starredStatus
                updateStarIcon()
                changesMade = true
                 }

            // Set up TextWatcher for editTitleEt
            binding.editTitleEt.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Enable or disable sendButton based on whether editTitleEt is empty or not
                    binding.saveButton.isEnabled = !s.isNullOrEmpty()
                    changesMade = true
                   }

                override fun afterTextChanged(s: Editable?) {}
            })

            // Set up TextWatcher for editDescriptionEt
            binding.editDescriptionEt.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    changesMade = true
                  }

                override fun afterTextChanged(s: Editable?) {}
            })

            binding.saveButton.setOnClickListener {
                val newTitle = binding.editTitleEt.text.toString()
                val newDescription = binding.editDescriptionEt.text.toString()

                setTaskData(
                    taskId,
                    newTitle,
                    newDescription,
                    taskCompleted,
                    newStarred,
                    newSelectedTaskCategories
                )

                activity?.supportFragmentManager?.popBackStack()
            }

            binding.markCompletedIcon.setOnClickListener {
                val newCompletedStatus = !taskCompleted
                val newTitle = binding.editTitleEt.text.toString()
                val newDescription = binding.editDescriptionEt.text.toString()

                setTaskData(
                    taskId,
                    newTitle,
                    newDescription,
                    newCompletedStatus,
                    newStarred,
                    newSelectedTaskCategories
                )

                activity?.supportFragmentManager?.popBackStack()
            }

            binding.deleteIcon.setOnClickListener {
                dialogManager.showTaskDeleteConfirmationDialog(
                    taskId,
                    this,
                    sharedViewModel,
                ) { isSuccessful ->
                    if (isSuccessful) {

                        // Remove the current fragment
                        activity?.supportFragmentManager?.popBackStack()
                    }

                }
            }

            binding.addCategory.setOnClickListener {
                dialogManager.showCategoriesDialog(
                    this,
                    sharedViewModel,
                    TAG,
                    newSelectedTaskCategories
                )
            }

            binding.popBack.setOnClickListener {
                popBackStackManager()
            }

            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                popBackStackManager()
            }

        } else {
            firebase.addLog("EditTaskFragment: selectedTaskData is null")
            binding.saveButton.isEnabled = false
        }
    }

    private fun setTaskData(
        id: String,
        title: String,
        description: String,
        completed: Boolean,
        starred: Boolean,
        selectedTaskCategories: List<CategoryEntity>
    ) {
        val categoryIds = selectedTaskCategories.map { categoryEntity ->
            categoryEntity.categoryId
        }
        // Call the ViewModel's method to update the task
        sharedViewModel.updateTask(
            id,
            title,
            description,
            completed,
            starred,
            categoryIds
        ) { isSuccessful ->
            if (isSuccessful) {
                // Handle success
                toastManager.showToast(fragmentContext, "Task updated successfully!")
            } else {
                // Handle failure
                toastManager.showToast(fragmentContext, "Failed to update task")
            }
        }
    }

    private fun updateStarIcon() {
        val starIcon = if (newStarred) R.drawable.star_filled else R.drawable.star
        binding.star.setImageResource(starIcon)
    }

    private fun popBackStackManager() {
        if (changesMade) {
            dialogManager.showDiscardDialog(this) { isSuccessful ->
                if (isSuccessful) {
                    // Reset changesMade flag
                    changesMade = false

                    // pop the back stack
                    parentFragmentManager.popBackStack()
                }
            }
        } else {
            // If no changes, simply pop the back stack
            parentFragmentManager.popBackStack()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save the state of changesMade and firstCategoryObservation
        outState.putBoolean("changesMade", changesMade)
        outState.putBoolean("firstCategoryObservation", firstCategoryObservation)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // Restore the state of changesMade and firstCategoryObservation
        changesMade = savedInstanceState?.getBoolean("changesMade") ?: false
        firstCategoryObservation =
            savedInstanceState?.getBoolean("userDetailsSetFromViewModel") ?: true
    }

    companion object {
        private const val TAG = "EditTaskFragment"
        private const val CATEGORY = "category"
        private const val CREATE = "create_new"
    }
}
