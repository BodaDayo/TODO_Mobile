package com.rgbstudios.todomobile.ui.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.tasks.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.data.entity.TaskEntity
import com.rgbstudios.todomobile.databinding.DialogDiscardTaskBinding
import com.rgbstudios.todomobile.databinding.DialogRemoveConfirmationBinding
import com.rgbstudios.todomobile.databinding.FragmentEditTaskBinding
import com.rgbstudios.todomobile.viewmodel.TodoViewModel


class EditTaskFragment : Fragment() {

    private val sharedViewModel: TodoViewModel by activityViewModels()
    private lateinit var binding: FragmentEditTaskBinding
    private lateinit var fragmentContext: Context
    private var changesMade = false
    private var deletedTask: TaskEntity? = null


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
            val taskId = selectedTaskData.taskId
            val taskCompleted = selectedTaskData.taskCompleted
            var newStarred = selectedTaskData.starred

            // Update the UI with the selected task data
            binding.editTitleEt.text =
                Editable.Factory.getInstance().newEditable(selectedTaskData.title)
            binding.editDescriptionEt.text =
                Editable.Factory.getInstance().newEditable(selectedTaskData.description)
            updateStarIcon(newStarred)

            // Check if the task is completed, change icon if true
            if (selectedTaskData.taskCompleted) {
                binding.markCompletedIcon.setImageResource(R.drawable.checkboxd)
                binding.markCompletedIcon.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        com.google.android.material.R.color.design_default_color_primary
                    )
                )
                binding.markCompletedTextView.text = getString(R.string.mark_as_uncompleted)
            } else {
                binding.markCompletedIcon.setImageResource(R.drawable.check)
                binding.markCompletedTextView.text = getString(R.string.mark_as_completed)
            }

            // set onClick listener for star button
            binding.star.setOnClickListener {
                newStarred = !newStarred // Toggle the starred status

                // Change the star icon based on the starredStatus
                updateStarIcon(newStarred)
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

                setTaskData(taskId, newTitle, newDescription, taskCompleted, newStarred)

                activity?.supportFragmentManager?.popBackStack()
            }

            binding.markCompletedTextView.setOnClickListener {
                val newCompletedStatus = !taskCompleted
                val newTitle = binding.editTitleEt.text.toString()
                val newDescription = binding.editDescriptionEt.text.toString()

                setTaskData(taskId, newTitle, newDescription, newCompletedStatus, newStarred)

                activity?.supportFragmentManager?.popBackStack()
            }

            binding.deleteTaskTextView.setOnClickListener {
                showDeleteConfirmationDialog(requireContext(), taskId)
            }

            binding.popBack.setOnClickListener {
                popBackStackManager()
            }

            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                popBackStackManager()
            }

        } else {
            Log.e("EditTaskFragment", "selectedTaskData is null")
            binding.saveButton.isEnabled = false
        }
    }

    private fun showDiscardDialog() {
        val dialogBinding = DialogDiscardTaskBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnDiscardConfirm.setOnClickListener {

            // Reset changesMade flag
            changesMade = false

            // Dismiss the dialog
            dialog.dismiss()

            // pop the back stack
            parentFragmentManager.popBackStack()
        }

        dialogBinding.btnDiscardCancel.setOnClickListener {
            // Dismiss the dialog
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(etContext: Context, Id: String) {
        val dialogBinding = DialogRemoveConfirmationBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.removeBody.text = getString(R.string.delete_task)

        val errorColor = ContextCompat.getColor(
            requireContext(),
            com.google.android.material.R.color.design_error
        )
        dialogBinding.btnConfirm.setTextColor(errorColor)
        dialogBinding.btnConfirm.text = getString(R.string.delete)

        dialogBinding.btnConfirm.setOnClickListener {

            // Store a reference to the deleted task before deleting it
            deletedTask = sharedViewModel.selectedTaskData.value

            // Call the ViewModel's logout method to sign out the user
            sharedViewModel.deleteTask(Id) { isSuccessful ->
                if (isSuccessful) {

                    // Dismiss the dialog
                    dialog.dismiss()

                    val snackBar = view?.let {
                        Snackbar.make(
                            it,
                            "Task deleted successfully!",
                            Snackbar.LENGTH_LONG
                        )
                    }
                    snackBar?.setAction("Undo") {
                        // Restore the deleted task
                        deletedTask?.let {
                            sharedViewModel.saveTask(it.title, it.description, it.starred) { isSuccessful ->
                                if (isSuccessful) {
                                    // Handle success
                                    Toast.makeText(etContext, "Task Restored!", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Handle failure
                                    Toast.makeText(etContext, "Failed to restore task", Toast.LENGTH_SHORT).show()
                                }
                            }
                          deletedTask = null
                        }
                    }
                    // Show the snackBar
                    snackBar?.show()

                    // Remove the current fragment
                    activity?.supportFragmentManager?.popBackStack()
                } else {
                    Toast.makeText(fragmentContext, "Failed to delete task", Toast.LENGTH_SHORT)
                        .show()
                }
            }

        }

        dialogBinding.btnCancel.setOnClickListener {
            // Dismiss the dialog
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun setTaskData(
        id: String,
        title: String,
        description: String,
        completed: Boolean,
        starred: Boolean
    ) {

        // Call the ViewModel's method to update the task
        sharedViewModel.updateTask(id, title, description, completed, starred) { isSuccessful ->
            if (isSuccessful) {
                // Handle success
                Toast.makeText(fragmentContext, "Task updated successfully!", Toast.LENGTH_SHORT)
                    .show()
            } else {
                // Handle failure
                Toast.makeText(fragmentContext, "Failed to update task", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStarIcon(newStarred: Boolean) {
        val starIcon = if (newStarred) R.drawable.star_filled else R.drawable.star
        binding.star.setImageResource(starIcon)
    }

    private fun popBackStackManager() {
        if (changesMade) {
            showDiscardDialog()
        } else {
            // If no changes, simply pop the back stack
            parentFragmentManager.popBackStack()
        }
    }
}
