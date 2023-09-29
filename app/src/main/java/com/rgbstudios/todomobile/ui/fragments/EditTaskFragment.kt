package com.rgbstudios.todomobile.ui.fragments

import android.app.UiModeManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateUtils.formatDateTime
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    private val thisFragment = this

    // Variables to store data from selected data
    private var taskId = ""
    private var taskCompleted = false
    private var newStarred = false
    private var dueDateTime: Calendar? = null
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

        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

        if (selectedTaskData != null) {

            binding.apply {

                // Get data from selected data
                taskId = selectedTaskData.taskId
                taskCompleted = selectedTaskData.taskCompleted
                newStarred = selectedTaskData.starred
                dueDateTime = selectedTaskData.dueDateTime

                // Update the UI with the selected task data
                editTitleEt.text =
                    Editable.Factory.getInstance().newEditable(selectedTaskData.title)

                editDescriptionEt.text =
                    Editable.Factory.getInstance().newEditable(selectedTaskData.description)

                updateStarIcon()

                editFragmentCategoryAdapter = EditFragmentCategoryAdapter(
                    fragmentContext,
                    sharedViewModel
                )
                tagRecyclerView.layoutManager =
                    LinearLayoutManager(fragmentContext, LinearLayoutManager.HORIZONTAL, false)
                tagRecyclerView.adapter = editFragmentCategoryAdapter

                sharedViewModel.selectedTaskCategories.observe(viewLifecycleOwner) { selectedTaskCategories ->
                    if (selectedTaskCategories.isNotEmpty()) {
                        newSelectedTaskCategories = selectedTaskCategories

                        editFragmentCategoryAdapter.updateTaskLists(selectedTaskCategories)

                        // Refresh the categoriesList
                        sharedViewModel.startDatabaseListeners()

                        tagRecyclerView.visibility = View.VISIBLE

                        changesMade = if (firstCategoryObservation) {
                            firstCategoryObservation = false
                            false
                        } else {
                            true
                        }
                    } else {
                        tagRecyclerView.visibility = View.GONE
                    }
                }

                if (dueDateTime != null) {
                    updateTaskDateTimeView(dueDateTime!!)
                } else {
                    // Set the date
                    taskDateTimeView.text = getString(R.string.set_date_time)

                    // Get the stroke color
                    val colorResourceId = if (isNightMode) {
                        R.color.myOnSurfaceNight // Use night mode color resource
                    } else {
                        R.color.myOnSurfaceDay // Use regular color resource
                    }
                    taskDateTimeView.setTextColor(
                        ContextCompat.getColor(
                            fragmentContext,
                            colorResourceId
                        )
                    )

                    taskDateTimeView.setBackgroundResource(R.drawable.rounded_corners)
                }

                // Check if the task is completed, change icon if true
                if (selectedTaskData.taskCompleted) {
                    markCompletedIcon.setImageResource(R.drawable.checkboxd)
                    markCompletedIcon.setColorFilter(
                        ContextCompat.getColor(
                            fragmentContext,
                            com.google.android.material.R.color.design_default_color_primary
                        )
                    )
                } else {
                    markCompletedIcon.setImageResource(R.drawable.check)
                }

                // set onClick listener for star button
                star.setOnClickListener {
                    newStarred = !newStarred // Toggle the starred status

                    // Change the star icon based on the starredStatus
                    updateStarIcon()
                    changesMade = true
                }

                // Set up TextWatcher for editTitleEt
                editTitleEt.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        // Enable or disable sendButton based on whether editTitleEt is empty or not
                        saveButton.isEnabled = !s.isNullOrEmpty()
                        changesMade = true
                    }

                    override fun afterTextChanged(s: Editable?) {}
                })

                // Set up TextWatcher for editDescriptionEt
                editDescriptionEt.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        changesMade = true
                    }

                    override fun afterTextChanged(s: Editable?) {}
                })

                saveButton.setOnClickListener {
                    val newTitle = editTitleEt.text.toString()
                    val newDescription = editDescriptionEt.text.toString()

                    // Check if the title is empty before proceeding
                    if (newTitle.isBlank()) {
                        // Show a toast or perform any other appropriate action to notify the user
                        toastManager.showShortToast(requireContext(),"Title cannot be empty!")
                        return@setOnClickListener
                    }

                    setTaskData(
                        taskId,
                        newTitle,
                        newDescription,
                        taskCompleted,
                        newStarred,
                        dueDateTime,
                        newSelectedTaskCategories
                    )

                    activity?.supportFragmentManager?.popBackStack()
                }

                markCompletedIcon.setOnClickListener {
                    val newCompletedStatus = !taskCompleted
                    val newTitle = editTitleEt.text.toString()
                    val newDescription = editDescriptionEt.text.toString()

                    if (newCompletedStatus) {
                        dueDateTime = null
                    }

                    setTaskData(
                        taskId,
                        newTitle,
                        newDescription,
                        newCompletedStatus,
                        newStarred,
                        dueDateTime,
                        newSelectedTaskCategories
                    )

                    activity?.supportFragmentManager?.popBackStack()
                }

                deleteIcon.setOnClickListener {
                    dialogManager.showTaskDeleteConfirmationDialog(
                        thisFragment,
                        sharedViewModel,
                    ) { isSuccessful ->
                        if (isSuccessful) {
                            // Remove the current fragment
                            activity?.supportFragmentManager?.popBackStack()
                        }

                    }
                }

                addCategory.setOnClickListener {
                    dialogManager.showCategoriesDialog(
                        thisFragment,
                        sharedViewModel,
                        TAG,
                        newSelectedTaskCategories
                    ) { }
                }

                editTaskDateLayout.setOnClickListener {
                    showDatePickerDialog()
                }

                popBack.setOnClickListener {
                    popBackStackManager()
                }

                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                    popBackStackManager()
                }
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
        dueDateTime: Calendar?,
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
            dueDateTime,
            categoryIds
        ) { isSuccessful ->
            if (isSuccessful) {
                // Handle success
                toastManager.showShortToast(fragmentContext, "Task updated successfully!")
            } else {
                // Handle failure
                toastManager.showShortToast(fragmentContext, "Failed to update task")
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

    private fun showDatePickerDialog() {
        dialogManager.showDatePickerDialog(this, dueDateTime) { selectedDate ->
            if (selectedDate != null) {
                dialogManager.showTimePickerDialog(this, dueDateTime, selectedDate) {
                    dueDateTime = it

                    if (dueDateTime != null) {
                        updateTaskDateTimeView(dueDateTime!!)
                    }
                }
            }
        }
    }

    private fun updateTaskDateTimeView(taskDateTime: Calendar) {
        val formattedDateTime = formatDateTime(taskDateTime)
        val currentTime = Calendar.getInstance()
        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

        if (taskDateTime.before(currentTime)) {
            // Set the date
            val overdueTime = formattedDateTime + getString(R.string.overdue_task)
            binding.taskDateTimeView.text = overdueTime

            // Set the text color to R.color.poor
            binding.taskDateTimeView.setTextColor(
                ContextCompat.getColor(
                    fragmentContext,
                    R.color.poor
                )
            )
        } else {
            // Set the date
            binding.taskDateTimeView.text = formattedDateTime

            // Set the text color to the default color
            val colorResourceId = if (isNightMode) {
                R.color.myOnSurfaceNight // Use night mode color resource
            } else {
                R.color.myOnSurfaceDay // Use regular color resource
            }
            binding.taskDateTimeView.setTextColor(
                ContextCompat.getColor(
                    fragmentContext,
                    colorResourceId
                )
            )
            binding.taskDateTimeView.setBackgroundResource(R.drawable.transparent_rectangle_background)
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
