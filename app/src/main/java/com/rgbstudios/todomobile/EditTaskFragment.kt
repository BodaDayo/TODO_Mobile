package com.rgbstudios.todomobile

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.rgbstudios.todomobile.databinding.FragmentEditTaskBinding
import com.rgbstudios.todomobile.model.TaskData
import com.rgbstudios.todomobile.model.TaskDataFromFirebase
import com.rgbstudios.todomobile.model.TaskViewModel


class EditTaskFragment : Fragment() {

    private val sharedViewModel: TaskViewModel by activityViewModels()
    private lateinit var binding: FragmentEditTaskBinding
    private lateinit var fragmentContext: Context
    private lateinit var menuProvider: MenuProvider

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
            var taskCompleted = selectedTaskData.taskCompleted

            // Update the UI with the selected task data
            binding.editTitleEt.text = Editable.Factory.getInstance().newEditable(selectedTaskData.title)
            binding.editDescriptionEt.text = Editable.Factory.getInstance().newEditable(selectedTaskData.description)

            // Set up TextWatcher for editTitleEt
            binding.editTitleEt.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Enable or disable sendButton based on whether editTitleEt is empty or not
                    binding.sendButton.isEnabled = !s.isNullOrEmpty()
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            binding.sendButton.setOnClickListener {
                val newTitle = binding.editTitleEt.text.toString()
                val newDescription = binding.editDescriptionEt.text.toString()

                setTaskData(taskId, newTitle, newDescription, taskCompleted)

                activity?.supportFragmentManager?.popBackStack()
            }
            binding.markCompletedTextView.setOnClickListener {
                taskCompleted = true
                val newTitle = binding.editTitleEt.text.toString()
                val newDescription = binding.editDescriptionEt.text.toString()

                setTaskData(taskId, newTitle, newDescription, taskCompleted)

                activity?.supportFragmentManager?.popBackStack()
            }

            setupMenu()
        } else {
            Log.e("EditTaskFragment", "selectedTaskData is null")
            binding.sendButton.isEnabled = false
        }

    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                // Handle for example visibility of menu items
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.edit_task_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_delete -> {
                        removeFragment()
                        true
                    }
                    // Add other menu item cases if needed
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun removeFragment() {
        // Remove the current fragment from its parent's fragment manager
        parentFragmentManager.beginTransaction().remove(this).commit()
        Log.d("popback", "wedgrmhfgjlvdZETXRTKG,VUTCHFXDZDGXNFCG")
        // Pop back to the HomeFragment
        parentFragmentManager.popBackStack("HomeFragment", FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }



    private fun setTaskData(id: String, title: String, description: String, completed: Boolean) {

        // Call the ViewModel's method to update the task
        sharedViewModel.updateTask(id, title, description, completed) { isSuccessful ->
            if (isSuccessful) {
                // Handle success
                Toast.makeText(fragmentContext, "Task updated successfully!", Toast.LENGTH_SHORT).show()
            } else {
                // Handle failure
                Toast.makeText(fragmentContext, "Failed to update task", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
