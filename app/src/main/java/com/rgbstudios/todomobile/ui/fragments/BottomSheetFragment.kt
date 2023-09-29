package com.rgbstudios.todomobile.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.databinding.FragmentBottomSheetBinding
import com.rgbstudios.todomobile.utils.DialogManager
import com.rgbstudios.todomobile.utils.ToastManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentBottomSheetBinding
    private lateinit var listener: AddTaskBtnClickListener
    private val dialogManager = DialogManager()
    private val toastManager = ToastManager()
    private var selectedDateTimeValue: Calendar? = null

    fun setListener(listener: AddTaskBtnClickListener) {
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

        binding.apply {

            star.setOnClickListener {
                starredStatus = !starredStatus // Toggle the starred status

                // Change the star icon based on the starredStatus
                val starIcon = if (starredStatus) R.drawable.star_filled else R.drawable.star
                star.setImageResource(starIcon)
            }

            taskDateTimeIcon.setOnClickListener {
                showDatePickerDialog()
            }

            taskDateTimeTVB.setOnClickListener {
                showDatePickerDialog()
            }

            addDescriptionBtn.setOnClickListener {
                taskDescriptionEt.visibility = View.VISIBLE
                taskDescriptionEt.requestFocus()
            }

            // Set up TextWatcher for editTitleEt
            taskTitleEt.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Enable or disable sendButton based on whether editTitleEt is empty or not
                    saveTask.isEnabled = !s.isNullOrEmpty()

                    // To change the color to gray when disabled
                    val colorRes =
                        if (s.isNullOrEmpty()) androidx.appcompat.R.color.material_grey_600 else R.color.myPrimary
                    saveTask.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            colorRes
                        )
                    )
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            saveTask.setOnClickListener {
                val titleEditText = taskTitleEt.text.toString()

                // Check if the title is empty before proceeding
                if (titleEditText.isBlank()) {
                    // Show a toast or perform any other appropriate action to notify the user
                    toastManager.showShortToast(requireContext(), "Title cannot be empty!")
                    return@setOnClickListener

                }

                val descriptionEditText = taskDescriptionEt.text.toString()

                // Clear the EditText fields
                taskTitleEt.text = null
                taskDescriptionEt.text = null

                listener.onSaveTask(
                    titleEditText,
                    descriptionEditText,
                    starredStatus,
                    selectedDateTimeValue
                )

            }
        }
    }

    private fun showDatePickerDialog() {
        dialogManager.showDatePickerDialog(this, null) { selectedDate ->
            if (selectedDate != null) {
                dialogManager.showTimePickerDialog(this, null, selectedDate) {
                    selectedDateTimeValue = it

                    if (selectedDateTimeValue != null) {
                        val formattedDateTime = formatDateTime(selectedDateTimeValue!!)
                        binding.taskDateTimeTVB.text = formattedDateTime
                        binding.taskDateTimeTVB.visibility = View.VISIBLE
                    }
                }
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

    private fun showDiscardDialog() {
        dialogManager.showDiscardDialog(this) { isSuccessful ->
            if (isSuccessful) {
                // Clear the title and description EditText fields
                binding.taskTitleEt.text?.clear()
                binding.taskDescriptionEt.text?.clear()
            }
        }
    }

    interface AddTaskBtnClickListener {
        fun onSaveTask(
            title: String,
            description: String,
            starred: Boolean,
            dateTimeValue: Calendar?
        )
    }
}