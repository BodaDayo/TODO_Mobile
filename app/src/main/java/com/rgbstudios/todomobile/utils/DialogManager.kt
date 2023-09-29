package com.rgbstudios.todomobile.utils

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.data.entity.CategoryEntity
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import com.rgbstudios.todomobile.databinding.DialogCategorySelectionBinding
import com.rgbstudios.todomobile.databinding.DialogDiscardTaskBinding
import com.rgbstudios.todomobile.databinding.DialogFeedbackBinding
import com.rgbstudios.todomobile.databinding.DialogForgotPasswordBinding
import com.rgbstudios.todomobile.databinding.DialogNewCategoryBinding
import com.rgbstudios.todomobile.databinding.DialogRemoveConfirmationBinding
import com.rgbstudios.todomobile.databinding.DialogRenameTaskBinding
import com.rgbstudios.todomobile.databinding.DialogSortingBinding
import com.rgbstudios.todomobile.model.TaskList
import com.rgbstudios.todomobile.ui.adapters.CategoryAdapter
import com.rgbstudios.todomobile.ui.adapters.CategoryColorAdapter
import com.rgbstudios.todomobile.ui.adapters.CategoryIconAdapter
import com.rgbstudios.todomobile.ui.adapters.EmojiAdapter
import com.rgbstudios.todomobile.viewmodel.TodoViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DialogManager {

    private val colorManager = ColorManager()
    private val iconManager = IconManager()
    private val categoryManager = CategoryManager()
    private val toastManager = ToastManager()
    private var delayedFeedbackHandler: Handler? = null
    private val firebase = FirebaseAccess()

    fun showCategoriesDialog(
        fragment: Fragment,
        viewModel: TodoViewModel,
        tag: String,
        selectedTaskCategories: List<CategoryEntity>?,
        callback: (Boolean) -> Unit
    ) {
        val context = fragment.context
        val layoutInflater = fragment.layoutInflater

        if (context != null) {
            // Create the categories dialog with ViewBinding
            val dialogBinding = DialogCategorySelectionBinding.inflate(layoutInflater)
            val dialog = Dialog(context)
            dialog.setContentView(dialogBinding.root)

            dialogBinding.apply {
                val adapter =
                    CategoryAdapter(
                        iconManager,
                        colorManager,
                        dialog,
                        dialogBinding,
                        object : CategoryAdapter.CategoryClickListener {
                            override fun onCategoryClick(category: CategoryEntity, dialog: Dialog) {
                                // Dismiss the dialog
                                dialog.dismiss()

                                // Handle the category click event with the CategoryEntity parameter
                                if (category.categoryId != CREATE) {

                                    when (tag) {
                                        HOME -> {
                                            viewModel.filterTasks(
                                                category.categoryId,
                                                CATEGORY
                                            ) {
                                                if (it) callback(true)
                                            }
                                        }

                                        BATCHADD -> {
                                            val taskList = viewModel.highlightedTaskList.value
                                            val categoryId = category.categoryId

                                            // Add category tags to all selected tasks
                                            val updatedTaskList = taskList?.map { taskEntity ->
                                                val updatedCategoryIds =
                                                    taskEntity.categoryIds + categoryId
                                                taskEntity.copy(categoryIds = updatedCategoryIds)
                                            }

                                            // Save updated tasks to database
                                            viewModel.saveMultipleTask(
                                                updatedTaskList ?: emptyList()
                                            ) { success ->
                                                if (success) {
                                                    callback(true)
                                                    val successString = taskList?.let {
                                                        fragment.resources.getQuantityString(
                                                            R.plurals.multiple_tasks_to_category_success,
                                                            it.size,
                                                            it.size
                                                        )
                                                    }
                                                    toastManager.showShortToast(
                                                        context,
                                                        successString!!
                                                    )
                                                } else {
                                                    toastManager.showShortToast(
                                                        context,
                                                        "Failed to add tasks to category"
                                                    )
                                                }
                                            }
                                        }

                                        else -> {
                                            viewModel.addTaskToCategory(category) { isSuccessful ->
                                                if (isSuccessful) {
                                                    // Handle success
                                                    toastManager.showShortToast(
                                                        context,
                                                        "${category.categoryName} tag added to task"
                                                    )
                                                } else {
                                                    // Handle failure
                                                    toastManager.showShortToast(
                                                        context,
                                                        "Failed to add ${category.categoryName} tag to task"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // show new category dialog
                                    showCreateCategoryDialog(
                                        fragment,
                                        viewModel,
                                    )
                                }
                            }

                            override fun onCategoryLongClick(
                                category: CategoryEntity,
                                dialog: Dialog,
                                dialogBinding: DialogCategorySelectionBinding
                            ) {
                                if (tag == BATCHADD || category.categoryId == CREATE) {
                                    return
                                }
                                dialogBinding.apply {
                                    editIcon.visibility = View.VISIBLE
                                    deleteIcon.visibility = View.VISIBLE

                                    editIcon.setOnClickListener {
                                        // show new category dialog
                                        showEditCategoryDialog(
                                            category,
                                            fragment,
                                            viewModel,
                                        )
                                    }

                                    deleteIcon.setOnClickListener {
                                        // Dismiss the dialog
                                        dialog.dismiss()

                                        // Call the ViewModel's deleteTask method
                                        viewModel.deleteCategory(category.categoryId) { isSuccessful ->
                                            if (isSuccessful) {

                                                val snackBar = fragment.view?.let {
                                                    Snackbar.make(
                                                        it,
                                                        "Category deleted Successfully!",
                                                        Snackbar.LENGTH_LONG
                                                    )
                                                }
                                                snackBar?.setAction("Undo") {
                                                    // Restore the deleted task
                                                    category.let {
                                                        viewModel.saveCategory(
                                                            it.categoryName,
                                                            it.categoryIconIdentifier,
                                                            it.categoryColorIdentifier
                                                        ) { isSuccessful ->
                                                            if (isSuccessful) {
                                                                // Handle success
                                                                toastManager.showShortToast(
                                                                    context,
                                                                    "Category Restored!"
                                                                )
                                                            } else {
                                                                // Handle failure
                                                                toastManager.showShortToast(
                                                                    context,
                                                                    "Failed to restore Category"
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                // Show the snackBar
                                                snackBar?.show()

                                            } else {
                                                toastManager.showShortToast(
                                                    context, "Failed to delete category"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                categoryRecyclerView.layoutManager = GridLayoutManager(context, 3) // 3 columns
                categoryRecyclerView.adapter = adapter

                // Set dialog title if adding multiple tasks to category
                if (tag == BATCHADD) {
                    val taskList = viewModel.highlightedTaskList.value
                    // Update showCategoryTitle tasks text with string plurals template
                    if (taskList != null) {
                        showCategoryTitle.text = fragment.resources.getQuantityString(
                            R.plurals.multiple_tasks_to_category,
                            taskList.size,
                            taskList.size
                        )
                    }

                    val categories = viewModel.categories.value
                    if (categories != null) {
                        adapter.updateCategoryList(categories)
                    }

                } else {

                    viewModel.categories.observe(fragment.viewLifecycleOwner) { list ->
                        // Add a "create new" category to the categories list
                        val createNewCategory = categoryManager.newCategory

                        val updatedCategories = if (tag == HOME) {
                            list.toMutableList()
                        } else {
                            val taskCategoryIds =
                                selectedTaskCategories!!.map { it.categoryId }.toSet()
                            list.filterNot { it.categoryId in taskCategoryIds }.toMutableList()
                        }

                        updatedCategories.add(createNewCategory)
                        adapter.updateCategoryList(updatedCategories)
                    }
                }
            }
            dialog.show()
        }
    }

    fun showCreateCategoryDialog(
        fragment: Fragment,
        viewModel: TodoViewModel,
    ) {
        val context = fragment.context
        val layoutInflater = fragment.layoutInflater

        if (context != null) {
            val dialogBinding = DialogNewCategoryBinding.inflate(layoutInflater)

            // Create a dialog using MaterialAlertDialogBuilder and set the custom ViewBinding layout
            val dialog = MaterialAlertDialogBuilder(context)
                .setView(dialogBinding.root)
                .create()

            // Variable to store the selected icon
            var selectedCategoryIcon: String? = null

            // Variable to store the selected color
            var selectedCategoryColor: String? = null

            // Get the icons and colors
            val iconList = iconManager.getAllIcons()
            val defaultIcon = iconManager.getDefaultIcon()
            val colorList = colorManager.getAllColors()
            val defaultColor = colorManager.getDefaultColor()

            dialogBinding.apply {

                val iconAdapter =
                    CategoryIconAdapter(
                        iconList,
                        iconManager,
                        object : CategoryIconAdapter.IconClickListener {
                            override fun onIconClick(iconIdentifier: String) {
                                selectedCategoryIcon = iconIdentifier

                                val iconResource =
                                    iconManager.getIconDrawableResource(iconIdentifier)

                                iconRecyclerView.visibility = View.GONE
                                categoryIconTV.visibility = View.GONE

                                // Show the selected Icon
                                categoryIcon.setImageResource(iconResource)
                                categoryIcon.visibility = View.VISIBLE
                            }

                        }
                    )
                iconRecyclerView.layoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

                // Set the adapter for the iconRecyclerView
                iconRecyclerView.adapter = iconAdapter

                val colorAdapter =
                    CategoryColorAdapter(
                        colorList,
                        colorManager,
                        object : CategoryColorAdapter.ColorClickListener {
                            override fun onColorClick(colorIdentifier: String) {
                                // Handle the color click event and update the selected color
                                selectedCategoryColor = colorIdentifier
                            }
                        }
                    )
                colorRecyclerView.layoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

                // Set the adapter for the colorRecyclerView
                colorRecyclerView.adapter = colorAdapter

                categoryIconBackground.setOnClickListener {
                    iconRecyclerView.visibility = View.VISIBLE
                }

                btnCancel.setOnClickListener {
                    // Dismiss the dialog
                    dialog.dismiss()
                }

                btnSave.setOnClickListener {
                    val categoryName = categoryNameEt.text.toString()

                    // Check if the title is empty before proceeding
                    if (categoryName.isBlank()) {
                        toastManager.showShortToast(
                            context,
                            "Category name cannot be empty!"
                        )
                        return@setOnClickListener
                    }

                    val exemptionList = categoryManager.specialCategoryNames

                    if (exemptionList.contains(categoryName)) {
                        toastManager.showShortToast(
                            context,
                            "This category name is not allowed!"
                        )
                        return@setOnClickListener
                    }

                    val categoryIconIdentifier = selectedCategoryIcon ?: defaultIcon

                    val categoryColorIdentifier = selectedCategoryColor ?: defaultColor

                    // Call the viewModel method to save the new category
                    onSaveCategory(
                        categoryName,
                        categoryIconIdentifier,
                        categoryColorIdentifier,
                        context,
                        viewModel
                    )

                    // Clear the EditText field
                    categoryNameEt.text = null

                    // Dismiss the dialog
                    dialog.dismiss()

                }
            }
            dialog.show()
        }
    }

    fun showEditCategoryDialog(
        category: CategoryEntity?,
        fragment: Fragment,
        viewModel: TodoViewModel,
    ) {
        val context = fragment.context
        val layoutInflater = fragment.layoutInflater

        if (context != null) {

            val dialogBinding = DialogNewCategoryBinding.inflate(layoutInflater)

            // Create a dialog using MaterialAlertDialogBuilder and set the custom ViewBinding layout
            val dialog = MaterialAlertDialogBuilder(context)
                .setView(dialogBinding.root)
                .create()

            // Variable to store the selected icon
            var selectedCategoryIcon: String? = null

            // Variable to store the selected color
            var selectedCategoryColor: String? = null

            // Get the icons and colors
            val iconList = iconManager.getAllIcons()
            val defaultIcon = iconManager.getDefaultIcon()
            val colorList = colorManager.getAllColors()
            val defaultColor = colorManager.getDefaultColor()

            dialogBinding.apply {

                val iconAdapter =
                    CategoryIconAdapter(
                        iconList,
                        iconManager,
                        object : CategoryIconAdapter.IconClickListener {
                            override fun onIconClick(iconIdentifier: String) {
                                selectedCategoryIcon = iconIdentifier

                                val iconResource =
                                    iconManager.getIconDrawableResource(iconIdentifier)

                                iconRecyclerView.visibility = View.GONE
                                categoryIconTV.visibility = View.GONE

                                // Show the selected Icon
                                categoryIcon.setImageResource(iconResource)
                                categoryIcon.visibility = View.VISIBLE
                            }

                        }
                    )
                iconRecyclerView.layoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

                // Set the adapter for the iconRecyclerView
                iconRecyclerView.adapter = iconAdapter

                val colorAdapter =
                    CategoryColorAdapter(
                        colorList,
                        colorManager,
                        object : CategoryColorAdapter.ColorClickListener {
                            override fun onColorClick(colorIdentifier: String) {
                                // Handle the color click event and update the selected color
                                selectedCategoryColor = colorIdentifier
                            }
                        }
                    )
                colorRecyclerView.layoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

                // Set the adapter for the colorRecyclerView
                colorRecyclerView.adapter = colorAdapter

                // Set up the layout
                newCategoryTitleTV.text = context.getString(R.string.edit_category)
                categoryNameEt.text =
                    Editable.Factory.getInstance().newEditable(category!!.categoryName)

                val iconResource =
                    iconManager.getIconDrawableResource(category.categoryIconIdentifier)

                categoryIconTV.visibility = View.GONE

                // Show the selected Icon
                categoryIcon.setImageResource(iconResource)
                categoryIcon.visibility = View.VISIBLE
                iconRecyclerView.visibility = View.VISIBLE

                categoryIconBackground.setOnClickListener {
                    iconRecyclerView.visibility = View.VISIBLE
                }

                btnCancel.setOnClickListener {
                    // Dismiss the dialog
                    dialog.dismiss()
                    viewModel.startDatabaseListeners()
                }

                btnSave.setOnClickListener {
                    val categoryName = categoryNameEt.text.toString()

                    // Check if the title is empty before proceeding
                    if (categoryName.isBlank()) {
                        toastManager.showShortToast(
                            context,
                            "Category name cannot be empty!"
                        )
                        return@setOnClickListener
                    }

                    val categoryIconIdentifier = selectedCategoryIcon ?: defaultIcon

                    val categoryColorIdentifier = selectedCategoryColor ?: defaultColor

                    // Call the viewModel method to update category
                    onUpdateCategory(
                        categoryName,
                        categoryIconIdentifier,
                        categoryColorIdentifier,
                        context,
                        viewModel
                    )

                    // Clear the EditText field
                    categoryNameEt.text = null

                    // Dismiss the dialog
                    dialog.dismiss()
                    viewModel.startDatabaseListeners()
                }
            }
            dialog.show()
        }
    }

    fun showTaskDeleteConfirmationDialog(
        fragment: Fragment,
        viewModel: TodoViewModel,
        callback: (Boolean) -> Unit
    ) {
        val context = fragment.context
        val layoutInflater = fragment.layoutInflater

        if (context != null) {
            val dialogBinding = DialogRemoveConfirmationBinding.inflate(layoutInflater)
            val dialog = MaterialAlertDialogBuilder(context)
                .setView(dialogBinding.root)
                .create()

            dialogBinding.removeBody.text = fragment.getString(R.string.delete_task)

            val errorColor = ContextCompat.getColor(
                context,
                R.color.poor
            )
            dialogBinding.btnConfirm.setTextColor(errorColor)
            dialogBinding.btnConfirm.text = fragment.getString(R.string.delete)

            dialogBinding.btnConfirm.setOnClickListener {

                // Store a reference to the deleted task before deleting it
                val deletedTask = viewModel.selectedTaskData.value

                val taskId = deletedTask?.taskId

                // Call the ViewModel's deleteTask method
                if (taskId != null) {
                    viewModel.deleteTask(taskId) { isSuccessful ->
                        if (isSuccessful) {

                            // Dismiss the dialog
                            dialog.dismiss()

                            val snackBar = fragment.view?.let {
                                Snackbar.make(
                                    it,
                                    "Task deleted successfully!",
                                    Snackbar.LENGTH_LONG
                                )
                            }
                            snackBar?.setAction("Undo") {
                                // Restore the deleted task
                                deletedTask.let {
                                    viewModel.saveTask(
                                        it.title,
                                        it.description,
                                        it.starred,
                                        it.dueDateTime
                                    ) { isSuccessful ->
                                        if (isSuccessful) {
                                            // Handle success
                                            toastManager.showShortToast(
                                                context,
                                                "Task Restored!"
                                            )
                                        } else {
                                            // Handle failure
                                            toastManager.showShortToast(
                                                context,
                                                "Failed to restore task"
                                            )
                                        }
                                    }
                                }
                            }
                            // Show the snackBar
                            snackBar?.show()

                            callback(true)
                        } else {
                            toastManager.showShortToast(
                                context,
                                "Failed to delete task"
                            )
                        }
                    }
                }
            }
            dialogBinding.btnCancel.setOnClickListener {
                // Dismiss the dialog
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    fun showBatchDeleteConfirmationDialog(
        fragment: Fragment,
        viewModel: TodoViewModel,
        callback: (Boolean) -> Unit
    ) {
        val context = fragment.context ?: return
        val layoutInflater = fragment.layoutInflater
        val dialogBinding = DialogRemoveConfirmationBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogBinding.root)
            .create()

        val deletedTaskList = viewModel.highlightedTaskList.value

        dialogBinding.apply {
            val taskCount = deletedTaskList?.size ?: 0
            removeBody.text = fragment.resources.getQuantityString(
                R.plurals.multiple_tasks_delete,
                taskCount,
                taskCount
            )

            val errorColor = ContextCompat.getColor(context, R.color.poor)
            btnConfirm.setTextColor(errorColor)
            btnConfirm.text = fragment.getString(R.string.delete)

            btnConfirm.setOnClickListener {
                val taskIdList = deletedTaskList?.map { it.taskId }

                taskIdList?.let { ids ->
                    // Call the ViewModel's deleteTask method
                    viewModel.batchDeleteTask(ids) { isSuccessful ->
                        if (isSuccessful) {
                            dialog.dismiss()
                            callback(true)

                            val snackBar = fragment.view?.let {
                                Snackbar.make(
                                    it,
                                    "Tasks deleted successfully!",
                                    Snackbar.LENGTH_LONG
                                ).setAction("Undo") {
                                    // Restore the deleted tasks
                                    viewModel.saveMultipleTask(deletedTaskList) { isRestored ->
                                        if (isRestored) {
                                            toastManager.showShortToast(
                                                context,
                                                "Deleted tasks Restored!"
                                            )
                                        } else {
                                            toastManager.showShortToast(
                                                context,
                                                "Failed to restore tasks"
                                            )
                                        }
                                    }
                                }
                            }
                            snackBar?.show()
                        } else {
                            toastManager.showShortToast(context, "Failed to delete tasks")
                        }
                    }
                }
            }

            btnCancel.setOnClickListener {
                dialog.dismiss()
            }
        }
        dialog.show()
    }


    fun showBatchRemoveTagConfirmationDialog(
        fragment: Fragment,
        viewModel: TodoViewModel,
        callback: (Boolean) -> Unit
    ) {
        val context = fragment.context ?: return
        val layoutInflater = fragment.layoutInflater
        val dialogBinding = DialogRemoveConfirmationBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogBinding.root)
            .create()

        val categoryName = viewModel.highlightedListName.value
        val removeTaskList = viewModel.highlightedTaskList.value
        val categoryList = viewModel.categories.value
        val categoryId = categoryList?.find { it.categoryName == categoryName }?.categoryId

        val pluralString = fragment.resources.getQuantityString(
            R.plurals.multiple_tasks_remove,
            removeTaskList?.size ?: 0,
            removeTaskList?.size ?: 0,
            categoryName
        )

        dialogBinding.apply {
            removeBody.text = String.format(pluralString, removeTaskList?.size ?: 0, categoryName)
            btnConfirm.text = fragment.getString(R.string.sure_response)

            btnConfirm.setOnClickListener {
                categoryId?.let { catId ->
                    // Remove category tag from all selected tasks
                    val updatedTaskList = removeTaskList?.map { taskEntity ->
                        val updatedCategoryIds = taskEntity.categoryIds.filter { it != catId }
                        taskEntity.copy(categoryIds = updatedCategoryIds)
                    }

                    // Save updated tasks to database
                    viewModel.saveMultipleTask(updatedTaskList ?: emptyList()) { success ->
                        if (success) {
                            callback(true)
                            toastManager.showShortToast(context, "Tasks removal successful!")
                        } else {
                            toastManager.showShortToast(
                                context,
                                "Failed to remove tasks from category"
                            )
                        }
                    }
                }
                dialog.dismiss()
            }

            btnCancel.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }


    fun showDiscardDialog(
        fragment: Fragment,
        callback: (Boolean) -> Unit
    ) {
        val context = fragment.context
        val layoutInflater = fragment.layoutInflater

        if (context != null) {

            val dialogBinding = DialogDiscardTaskBinding.inflate(layoutInflater)
            val dialog = MaterialAlertDialogBuilder(context)
                .setView(dialogBinding.root)
                .create()

            dialogBinding.btnDiscardConfirm.setOnClickListener {

                // Dismiss the dialog
                dialog.dismiss()

                callback(true)

            }

            dialogBinding.btnDiscardCancel.setOnClickListener {
                // Dismiss the dialog
                dialog.dismiss()

                callback(false)
            }

            dialog.show()
        }
    }

    fun showLogoutConfirmationDialog(
        fragment: Fragment,
        viewModel: TodoViewModel,
        callback: (Boolean) -> Unit
    ) {
        val context = fragment.context
        val layoutInflater = fragment.layoutInflater

        if (context != null) {
            val dialogBinding = DialogRemoveConfirmationBinding.inflate(layoutInflater)
            val dialog = MaterialAlertDialogBuilder(context)
                .setView(dialogBinding.root)
                .create()

            dialogBinding.btnConfirm.setOnClickListener {
                // Call the ViewModel's logout method to sign out the user
                viewModel.logOut { logOutSuccessful, errorMessage ->

                    if (logOutSuccessful) {
                        // Dismiss the dialog
                        dialog.dismiss()

                        callback(true)

                    } else {
                        // Dismiss the dialog
                        dialog.dismiss()

                        errorMessage?.let { message ->
                            val output = message.substringAfter(": ")
                            toastManager.showLongToast(context, output)
                        }
                    }
                }
            }

            dialogBinding.btnCancel.setOnClickListener {
                // Dismiss the dialog
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    fun showForgotPasswordDialog(
        fragment: Fragment,
        auth: FirebaseAuth
    ) {
        val context = fragment.context
        val layoutInflater = fragment.layoutInflater

        if (context != null) {

            val dialogBinding = DialogForgotPasswordBinding.inflate(layoutInflater)

            val dialog = MaterialAlertDialogBuilder(context)
                .setView(dialogBinding.root)
                .create()

            dialogBinding.resetPasswordButton.setOnClickListener {
                val email = dialogBinding.emailEditText.text.toString().trim()

                if (email.isNotEmpty()) {
                    auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                toastManager.showLongToast(
                                    context,
                                    "Password reset link sent to your email"
                                )
                            } else {
                                toastManager.showShortToast(
                                    context,
                                    "Failed to send password reset email"
                                )
                            }
                            // Dismiss the dialog
                            dialog.dismiss()
                        }
                } else {
                    toastManager.showShortToast(
                        context,
                        "Please enter your email"
                    )
                }
            }

            dialog.show()
        }
    }

    fun showFeedbackDialog(
        fragment: Fragment,
        userEmail: String,
    ) {
        val context = fragment.context
        val layoutInflater = fragment.layoutInflater

        if (context != null) {
            val dialogBinding = DialogFeedbackBinding.inflate(layoutInflater)

            // Create a dialog using MaterialAlertDialogBuilder and set the custom ViewBinding layout
            val dialog = MaterialAlertDialogBuilder(context)
                .setView(dialogBinding.root)
                .create()

            // Variable to store the selected emoji details
            var selectedEmojiTriple: Triple<String, Int, Int>? = null


            // Get the emojis
            val emojiList = iconManager.getEmojiList()

            dialogBinding.apply {

                feedbackImageView.visibility = View.VISIBLE
                emojiSelectedBack.visibility = View.INVISIBLE
                emojiSelectedView.visibility = View.INVISIBLE
                feedbackThanks.visibility = View.INVISIBLE

                val emojiAdapter =
                    EmojiAdapter(
                        emojiList,
                        object : EmojiAdapter.EmojiClickListener {
                            override fun onEmojiClick(emojiTriple: Triple<String, Int, Int>) {
                                selectedEmojiTriple = emojiTriple
                            }

                        }
                    )
                emojiRecyclerView.setHasFixedSize(true)
                emojiRecyclerView.layoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

                // Set the adapter for the emojiRecyclerView
                emojiRecyclerView.adapter = emojiAdapter

                submitButton.setOnClickListener {
                    try {// Check if an emoji has been selected before proceeding
                        if (selectedEmojiTriple == null) {
                            toastManager.showLongToast(
                                context,
                                "Please rate your user experience using one of the emojis"
                            )
                            return@setOnClickListener
                        }
                        val emojiIdentifier = selectedEmojiTriple!!.first
                        val emojiIconResource = selectedEmojiTriple!!.second
                        val emojiColor = selectedEmojiTriple!!.third

                        val userRating =
                            emojiIdentifier.replaceFirstChar { it.uppercase() }.replace('_', ' ')

                        val userComment =
                            editCommentEt.text.toString().ifEmpty { "No additional comments" }

                        emojiSelectedView.setImageResource(emojiIconResource)

                        emojiSelectedBack.setColorFilter(
                            ContextCompat.getColor(context, emojiColor),
                            PorterDuff.Mode.SRC_IN
                        )

                        feedbackImageView.visibility = View.INVISIBLE
                        emojiSelectedBack.visibility = View.VISIBLE
                        emojiSelectedView.visibility = View.VISIBLE
                        feedbackThanks.visibility = View.VISIBLE

                        // Remove any previously posted callbacks to avoid multiple executions
                        delayedFeedbackHandler?.removeCallbacksAndMessages(null)

                        // Create a new Handler for delayed navigation
                        delayedFeedbackHandler = Handler(Looper.myLooper()!!)

                        delayedFeedbackHandler?.postDelayed({

                            val packageManager = context.packageManager
                            val packageName = context.packageName

                            val deviceModel = Build.MODEL
                            val androidVersion = Build.VERSION.RELEASE
                            val packageInfo = packageManager.getPackageInfo(packageName, 0)
                            val appVersion = packageInfo.versionName

                            // Get the current date and time
                            val currentTime = Calendar.getInstance()
                            val dateFormat = SimpleDateFormat(
                                "EEE, MMM dd, yyyy 'At' hh:mm a",
                                Locale.getDefault()
                            )
                            val formattedTime = dateFormat.format(currentTime.time)

                            // Create a StringBuilder to build the feedback message
                            val feedbackMessage = StringBuilder()
                            feedbackMessage.append("User Rating: $userRating\n")
                            feedbackMessage.append("User Comment: $userComment\n")
                            feedbackMessage.append("Device Model: $deviceModel\n")
                            feedbackMessage.append("Android Version: $androidVersion\n")
                            feedbackMessage.append("App Version: $appVersion\n\n")
                            feedbackMessage.append("Feedback sent by: $userEmail on: $formattedTime")

                            // Create an Intent to send feedback
                            val emailIntent = Intent(Intent.ACTION_SEND)
                            emailIntent.type = "text/plain"
                            emailIntent.putExtra(
                                Intent.EXTRA_EMAIL,
                                arrayOf("rgb.mobile.studios@gmail.com")
                            )
                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "User Feedback")
                            emailIntent.putExtra(Intent.EXTRA_TEXT, feedbackMessage.toString())

                            // Start the email client or any other app that can handle this intent
                            context.startActivity(
                                Intent.createChooser(
                                    emailIntent,
                                    "Send Feedback"
                                )
                            )

                            // Clear the EditText field
                            editCommentEt.text = null

                            // Dismiss the dialog
                            dialog.dismiss()
                        }, 1500)
                    } catch (e: Exception) {
                        firebase.recordCaughtException(e)

                        // Dismiss the dialog
                        dialog.dismiss()

                        toastManager.showLongToast(
                            context,
                            "Feedback Sending failed, something went wrong!"
                        )
                    }

                }
            }
            dialog.show()
        }

    }

    fun showSortingDialog(
        fragment: Fragment,
        viewModel: TodoViewModel,
        allTaskList: List<TaskList>?
    ) {
        val context = fragment.context ?: return
        val layoutInflater = fragment.layoutInflater
        val dialogBinding = DialogSortingBinding.inflate(layoutInflater)

        val sortingCondition = viewModel.sortingCondition.value ?: Pair(DATE, true)
        var (condition, order) = sortingCondition

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.apply {
            radioTitle.isChecked = condition == TITLE
            radioDate.isChecked = condition == DATE

            radioAscending.text = if (radioDate.isChecked) {
                context.getString(R.string.oldest_first)
            } else {
                context.getString(R.string.a_z)
            }

            radioDescending.text = if (radioDate.isChecked) {
                context.getString(R.string.newest_first)
            } else {
                context.getString(R.string.z_a)
            }

            radioSortBy.setOnCheckedChangeListener { _, checkedId ->
                condition = if (checkedId == R.id.radioTitle) TITLE else DATE
                updateRadioSortOrder(context, dialogBinding, condition, order)
            }

            radioSortOrder.setOnCheckedChangeListener { _, checkedId ->
                order = checkedId == R.id.radioAscending
            }

            btnConfirm.setOnClickListener {
                if (allTaskList != null) {
                    viewModel.sortAllTasksList(allTaskList, Pair(condition, order))
                }
                dialog.dismiss()
            }

            btnCancel.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    fun showDatePickerDialog(
        fragment: Fragment,
        dueDateTime: Calendar?,
        callback: (Calendar?) -> Unit
    ) {
        val context = fragment.context
        val calendar = Calendar.getInstance()

        // Set the initial date value to dueDateTime if it's not null
        if (dueDateTime != null) {
            calendar.timeInMillis = dueDateTime.timeInMillis
        }

        if (context != null) {
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    try {
                        // Handle selected date
                        calendar.set(year, month, dayOfMonth)

                        // Return selectedDate
                        callback(calendar)
                    } catch (e: Exception) {
                        callback(null)
                        toastManager.showShortToast(context, "Pick a valid Date")
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // Set the minimum date to the current date
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()

            datePickerDialog.show()
        }
    }

    fun showTimePickerDialog(
        fragment: Fragment,
        dueDateTime: Calendar?,
        selectedDate: Calendar,
        callback: (Calendar?) -> Unit
    ) {
        val context = fragment.context
        val calendar = Calendar.getInstance()

        // Add two hours to the current time
        calendar.add(Calendar.HOUR_OF_DAY, 2)

        // Set the initial time value to dueDateTime if it's not null
        if (dueDateTime != null) {
            calendar.timeInMillis = dueDateTime.timeInMillis
        }

        if (context != null) {
            val timePickerDialog = TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    try {
                        // Handle selected time
                        selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        selectedDate.set(Calendar.MINUTE, minute)

                        // Check if the selected time is not in the past (only if selected date is today)
                        val currentDate = Calendar.getInstance()
                        if (selectedDate.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR) &&
                            selectedDate.get(Calendar.DAY_OF_YEAR) == currentDate.get(Calendar.DAY_OF_YEAR) &&
                            selectedDate.before(currentDate)
                        ) {
                            toastManager.showShortToast(context, "Please select a future time")
                            return@TimePickerDialog
                        }

                        callback(selectedDate)
                    } catch (e: Exception) {
                        callback(null)
                        toastManager.showShortToast(context, "Pick a valid Time")
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            )

            timePickerDialog.show()
        }
    }

    fun showTaskRenameDialog(
        fragment: Fragment,
        viewModel: TodoViewModel,
        callback: (Boolean) -> Unit
    ) {
        val context = fragment.context
        val layoutInflater = fragment.layoutInflater
        val task = viewModel.highlightedTaskList.value?.first()

        if (context != null && task != null) {

            val dialogBinding = DialogRenameTaskBinding.inflate(layoutInflater)

            // Create a dialog using MaterialAlertDialogBuilder and set the custom ViewBinding layout
            val dialog = MaterialAlertDialogBuilder(context)
                .setView(dialogBinding.root)
                .create()

            dialogBinding.apply {
                // Update the UI with the selected task data
                renameTitleEt.text =
                    Editable.Factory.getInstance().newEditable(task.title)

                renameDescriptionEt.text =
                    Editable.Factory.getInstance().newEditable(task.description)

                openDescriptionBtn.setOnClickListener {
                    renameDescriptionLayout.visibility = View.VISIBLE
                    renameDescriptionEt.requestFocus()
                }

                saveRename.setOnClickListener {
                    val newTitle = renameTitleEt.text.toString()
                    val newDescription = renameDescriptionEt.text.toString()

                    // Check if the title is empty before proceeding
                    if (newTitle.isBlank()) {
                        toastManager.showShortToast(context, "Title cannot be empty!")
                        return@setOnClickListener
                    }
                    setTaskData(
                        task.taskId,
                        newTitle,
                        newDescription,
                        task.taskCompleted,
                        task.starred,
                        task.dueDateTime,
                        task.categoryIds,
                        context,
                        viewModel
                    ) {
                        if (it) {
                            dialog.dismiss()
                            callback(true)
                        }
                    }
                }
            }
            dialog.show()
        }
    }

    /**
     *-----------------------------------------------------------------------------------------------
     */

    private fun onSaveCategory(
        categoryName: String,
        categoryIconIdentifier: String,
        categoryColorIdentifier: String,
        context: Context,
        viewModel: TodoViewModel,
    ) {
        // Call the ViewModel's method to save the task
        viewModel.saveCategory(
            categoryName,
            categoryIconIdentifier,
            categoryColorIdentifier
        ) { isSuccessful ->
            if (isSuccessful) {
                // Handle success
                toastManager.showShortToast(
                    context,
                    "New category added successfully!"
                )
            } else {
                // Handle failure
                toastManager.showShortToast(
                    context,
                    "Failed to add new category"
                )
            }
        }

    }

    private fun onUpdateCategory(
        categoryName: String,
        categoryIconIdentifier: String,
        categoryColorIdentifier: String,
        context: Context,
        viewModel: TodoViewModel,
    ) {
        // Call the ViewModel's method to save the task
        viewModel.updateCategory(
            categoryName,
            categoryIconIdentifier,
            categoryColorIdentifier
        ) { isSuccessful ->
            if (isSuccessful) {
                // Handle success
                toastManager.showShortToast(
                    context,
                    "Category updated successfully!"
                )
            } else {
                // Handle failure
                toastManager.showShortToast(
                    context,
                    "Failed to update category"
                )
            }
        }

    }

    private fun updateRadioSortOrder(
        context: Context,
        binding: DialogSortingBinding,
        condition: String,
        order: Boolean
    ) {
        val radioTextMap = mapOf(
            TITLE to Pair(context.getString(R.string.a_z), context.getString(R.string.z_a)),
            DATE to Pair(
                context.getString(R.string.oldest_first),
                context.getString(R.string.newest_first)
            ),
        )

        val (ascendingText, descendingText) = radioTextMap[condition] ?: Pair("", "")

        binding.apply {
            radioAscending.text = ascendingText
            radioDescending.text = descendingText
            radioAscending.isChecked = order == true
            radioDescending.isChecked = order == false
        }
    }

    private fun setTaskData(
        id: String,
        title: String,
        description: String,
        completed: Boolean,
        starred: Boolean,
        dueDateTime: Calendar?,
        categoryIds: List<String>,
        context: Context,
        viewModel: TodoViewModel,
        callback: (Boolean) -> Unit
    ) {
        // Call the ViewModel's method to update the task
        viewModel.updateTask(
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
                toastManager.showShortToast(context, "Task updated successfully!")
                callback(true)
            } else {
                // Handle failure
                toastManager.showShortToast(context, "Failed to update task")
            }
        }
    }

    /**
     *-----------------------------------------------------------------------------------------------
     */

    companion object {
        private const val HOME = "HomeFragment"
        private const val CREATE = "create_new"
        private const val EDIT = "edit"
        private const val CATEGORY = "category"
        private const val DATE = "date"
        private const val TITLE = "title"
        private const val COMPLETED = "completed"
        private const val UNCOMPLETED = "uncompleted"
        private const val STAR = "Favorites"
        private const val SEARCH = "Search Results"
        private const val BATCHADD = "BatchAdd"
        private const val BATCHREMOVE = "BatchRemove"
    }
}