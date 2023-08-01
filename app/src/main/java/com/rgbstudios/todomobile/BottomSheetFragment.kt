package com.rgbstudios.todomobile


import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.internal.ViewUtils.hideKeyboard
import com.google.android.material.textfield.TextInputEditText
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

        registerEvents()
    }

    private fun registerEvents() {

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
                    if (s.isNullOrEmpty()) androidx.appcompat.R.color.material_grey_600 else com.google.android.material.R.color.design_default_color_primary
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
                binding.taskDescriptionEt
            )

        }
    }

    interface DialogAddTaskBtnClickListener {
        fun onSaveTask(
            title: String,
            description: String,
            titleEt: TextInputEditText,
            descriptionEt: TextInputEditText
        )
    }
}