package com.rgbstudios.todomobile


import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.rgbstudios.todomobile.databinding.DialogDiscardTaskBinding
import com.rgbstudios.todomobile.databinding.FragmentBottomSheetBinding

class BottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentBottomSheetBinding
    private lateinit var listener: DialogAddTaskBtnClickListener

    fun setListener(listener: DialogAddTaskBtnClickListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Intercept back button press and show discard dialog if title field is not empty
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                if (!binding.taskTitleEt.text.isNullOrEmpty()) {
                    showDiscardDialog()
                    return@setOnKeyListener true
                }
            }
            return@setOnKeyListener false
        }

        registerEvents()

    }

    private fun registerEvents() {

        var starredStatus = false

        binding.star.setOnClickListener {
            starredStatus = !starredStatus // Toggle the starred status

            // Change the star icon based on the starredStatus
            val starIcon = if (starredStatus) R.drawable.star_filled else R.drawable.star
            binding.star.setImageResource(starIcon)
        }

        binding.addDescriptionBtn.setOnClickListener {
            binding.taskDescriptionEt.visibility = View.VISIBLE
        }
        // Set up TextWatcher for editTitleEt
        binding.taskTitleEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Enable or disable sendButton based on whether editTitleEt is empty or not
                binding.saveTask.isEnabled = !s.isNullOrEmpty()

                // To change the color to gray when disabled
                val colorRes =
                    if (s.isNullOrEmpty()) androidx.appcompat.R.color.material_grey_600 else R.color.myPrimary
                binding.saveTask.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.saveTask.setOnClickListener {
            val titleEditText = binding.taskTitleEt.text.toString()

            // Check if the title is empty before proceeding
            if (titleEditText.isBlank()) {
                // Show a toast or perform any other appropriate action to notify the user
                Toast.makeText(requireContext(), "Title cannot be empty!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val descriptionEditText = binding.taskDescriptionEt.text.toString()

            listener.onSaveTask(
                titleEditText,
                descriptionEditText,
                binding.taskTitleEt,
                binding.taskDescriptionEt,
                starredStatus
            )

        }
    }

    private fun showDiscardDialog() {
        val dialogBinding = DialogDiscardTaskBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnDiscardConfirm.setOnClickListener {
            // Clear the title and description EditText fields
            binding.taskTitleEt.text?.clear()
            binding.taskDescriptionEt.text?.clear()

            // Dismiss the dialog
            dialog.dismiss()
        }

        dialogBinding.btnDiscardCancel.setOnClickListener {
            // Dismiss the dialog
            dialog.dismiss()
        }

        dialog.show()
    }


    interface DialogAddTaskBtnClickListener {
        fun onSaveTask(
            title: String,
            description: String,
            titleEt: TextInputEditText,
            descriptionEt: TextInputEditText,
            starred: Boolean
        )
    }
}