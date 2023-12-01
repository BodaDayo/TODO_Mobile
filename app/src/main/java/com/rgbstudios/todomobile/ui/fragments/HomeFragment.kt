package com.rgbstudios.todomobile.ui.fragments

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.TodoMobileApplication
import com.rgbstudios.todomobile.databinding.FragmentHomeBinding
import com.rgbstudios.todomobile.model.TaskList
import com.rgbstudios.todomobile.ui.adapters.ListAdapter
import com.rgbstudios.todomobile.utils.CategoryManager
import com.rgbstudios.todomobile.utils.DialogManager
import com.rgbstudios.todomobile.utils.ToastManager
import com.rgbstudios.todomobile.viewmodel.TodoViewModel
import com.rgbstudios.todomobile.viewmodel.TodoViewModelFactory
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment(),
    OnNavigationItemSelectedListener {

    private val sharedViewModel: TodoViewModel by activityViewModels {
        TodoViewModelFactory(activity?.application as TodoMobileApplication)
    }

    private lateinit var binding: FragmentHomeBinding
    private lateinit var taskListAdapter: ListAdapter
    private lateinit var bottomSheetFragment: BottomSheetFragment
    private lateinit var navDrawerLayout: DrawerLayout
    private lateinit var fragmentContext: Context
    private lateinit var callback: OnBackPressedCallback
    private val listener = this as OnNavigationItemSelectedListener
    private val dialogManager = DialogManager()
    private val toastManager = ToastManager()
    private val categoryManager = CategoryManager()
    private var isSearchResultsShowing = false
    private var userEmail: String? = null
    private var currentTasks: List<TaskList>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        fragmentContext = requireContext()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if it's the first launch
        val isFirstLaunch = sharedViewModel.isFirstLaunch.value ?: true

        if (isFirstLaunch) {
            // Show onboarding prompts
            showOnboardingPrompts()
        }

        init()
        updateFromDatabase()
        registerEvents()
    }

    private fun init() {

        binding.apply {

            // display avatar progress bar while waiting to load image
            avatarProgressBar.visibility = View.VISIBLE
            overlayView.visibility = View.VISIBLE
            tasksProgressBar.visibility = View.VISIBLE

            // Set up the adapter
            taskListAdapter = ListAdapter(fragmentContext, sharedViewModel)

            // Set up BottomSheetFragment
            bottomSheetFragment = BottomSheetFragment()
            bottomSheetFragment.setListener(
                object : BottomSheetFragment.AddTaskBtnClickListener {
                    override fun onSaveTask(
                        title: String,
                        description: String,
                        starred: Boolean,
                        dateTimeValue: Calendar?
                    ) {
                        // Call the ViewModel's method to save the task
                        sharedViewModel.saveTask(
                            title,
                            description,
                            starred,
                            dateTimeValue
                        ) { isSuccessful ->
                            if (isSuccessful) {
                                // Handle success
                                toastManager.showShortToast(
                                    fragmentContext,
                                    "Task saved successfully!"
                                )
                                emptinessLayout.visibility = View.INVISIBLE
                            } else {
                                // Handle failure
                                toastManager.showShortToast(
                                    fragmentContext,
                                    "Failed to save task"
                                )
                            }

                            bottomSheetFragment.dismiss()
                        }
                    }

                }
            )

            // Set up the nav drawer
            navDrawerLayout = drawerLayout

            //Set up back pressed call back
            callback =
                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                    val taskListName = sharedViewModel.highlightedListName.value

                    if (taskListName != null) {
                        taskListAdapter.clearTasksSelection(taskListName)
                    }
                }
            callback.isEnabled = false

            // Set up the recyclerView
            parentRecyclerView.setHasFixedSize(true)
            parentRecyclerView.layoutManager = LinearLayoutManager(context)
            parentRecyclerView.adapter = taskListAdapter


            // Set up listeners
            navigationView.setNavigationItemSelectedListener(listener)

            swipeRefreshLayout.setOnRefreshListener {
                // Update data from database when the user performs the pull-to-refresh action
                updateFromDatabase()
                exitSelectionMode()
            }

            // Set up SearchView's query text listener
            binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(query: String?): Boolean {
                    isSearchResultsShowing = if (query.isNullOrEmpty()) {

                        sharedViewModel.startDatabaseListeners()
                        false
                    } else {

                        sharedViewModel.filterTasks(query, SEARCH) {
                            if (it) upDateAdapterWithFilteredTasks()
                        }
                        true
                    }
                    return true
                }
            })

            // Set up LiveData Observers
            observeLiveData()

// ----------------------------------------------------------------------------------------------------
            // Set up notification badges

            // Access the menu
            val navigationView = binding.navigationView
            val menu = navigationView.menu

            // Find the menu item you want to add a badge to
            val settingMenuItem = menu.findItem(R.id.settings)

            // Inflate a custom layout for the action view (badge)
            val badgeLayout = layoutInflater.inflate(R.layout.notification_badge, navigationView, false)

            // Set the action view for the menu item
            settingMenuItem.actionView = badgeLayout

            // Reference the badge view within the action view
            val notificationBadge = badgeLayout.findViewById<ImageView>(R.id.notification_badge)

            // Update the visibility of settings badge based on authState
            sharedViewModel.isEmailAuthSet.observe(viewLifecycleOwner) { emailAuthIsSet ->
                if (emailAuthIsSet) {
                    notificationBadge.visibility = View.GONE
                    avatarHomeNB.visibility = View.GONE
                } else {
                    notificationBadge.visibility = View.VISIBLE
                    avatarHomeNB.visibility = View.VISIBLE
                }
            }

// ----------------------------------------------------------------------------------------------------

        }
    }

    private fun observeLiveData() {
        binding.apply {

            val uiModeManager =
                requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
            val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

            val navigationDrawerView = navigationView
            // Get the header view from NavigationView
            val headerView = navigationDrawerView.getHeaderView(0)

            val avatarNavDrw = headerView.findViewById<ImageView>(R.id.avatarNavDrw)
            val avatarProgressBarND = headerView.findViewById<ProgressBar>(R.id.avatarProgressBarNav)
            val overlayViewND = headerView.findViewById<View>(R.id.overlayViewNav)
            val nameNavDrw = headerView.findViewById<TextView>(R.id.userNameTxt)
            val emailNavDrw = headerView.findViewById<TextView>(R.id.emailTxt)
            val occupationNavDrw = headerView.findViewById<TextView>(R.id.occupationTxt)
            val uncompletedTasks =
                headerView.findViewById<TextView>(R.id.unCompletedTasksNumber)
            val completedTasks = headerView.findViewById<TextView>(R.id.completedTasksNumber)
            val progressBarNavDrw = headerView.findViewById<View>(R.id.progressBar)
            val progressBackNavDrw = headerView.findViewById<View>(R.id.progressBackground)

            avatarProgressBarND.visibility = View.VISIBLE
            overlayViewND.visibility = View.VISIBLE

            // Set up OnClickListener for the navigation drawer header
            headerView.setOnClickListener {
                navDrawerLayout.closeDrawer(GravityCompat.START)
                findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
            }

            // ItemCountLayout Views

            selectAllTasks.setOnClickListener {
                val taskList = sharedViewModel.highlightedTaskList.value
                val taskListName = sharedViewModel.highlightedListName.value

                if (taskList != null && taskListName != null) {
                    // Find the TaskList with the matching name
                    val matchingTaskList = currentTasks?.find { it.name == taskListName }

                    if (matchingTaskList != null) {
                        val selectedList = matchingTaskList.list
                        val isFilledList = taskList.size == selectedList.size

                        // Pass the current state to adapter
                        if (isFilledList) {
                            taskListAdapter.clearTasksSelection(taskListName)
                        } else {
                            taskListAdapter.fillTasKSelection(taskListName, selectedList)
                        }

                    }
                }
            }

            closeSelection.setOnClickListener {
                val taskListName = sharedViewModel.highlightedListName.value

                if (taskListName != null) {
                    taskListAdapter.clearTasksSelection(taskListName)
                }
            }

            // RefactorTaskLayout views

            deleteSelected.setOnClickListener {
                dialogManager.showBatchDeleteConfirmationDialog(
                    this@HomeFragment,
                    sharedViewModel
                ) {
                    if (it) exitSelectionMode()
                }
            }

            renameSelected.setOnClickListener {
                dialogManager.showTaskRenameDialog(this@HomeFragment, sharedViewModel) {
                    if (it) exitSelectionMode()
                }
            }

            editDateTime.setOnClickListener {
                val task = sharedViewModel.highlightedTaskList.value?.first()
                val taskDueDateTime = task?.dueDateTime
                if (task != null) {
                    dialogManager.showDatePickerDialog(
                        this@HomeFragment,
                        taskDueDateTime
                    ) { selectedDate ->
                        if (selectedDate != null) {
                            dialogManager.showTimePickerDialog(
                                this@HomeFragment,
                                taskDueDateTime,
                                selectedDate
                            ) {

                                if (it != null) {
                                    // Call the ViewModel's method to update the task
                                    sharedViewModel.updateTask(
                                        task.taskId,
                                        task.title,
                                        task.description,
                                        task.taskCompleted,
                                        task.starred,
                                        it,
                                        task.categoryIds
                                    ) { isSuccessful ->
                                        if (isSuccessful) {
                                            // Handle success
                                            toastManager.showShortToast(
                                                fragmentContext,
                                                "Task updated successfully!"
                                            )
                                            exitSelectionMode()
                                        } else {
                                            // Handle failure
                                            toastManager.showShortToast(
                                                fragmentContext,
                                                "Failed to update task"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            addToCategory.setOnClickListener {

                val taskListName = sharedViewModel.highlightedListName.value
                val exemptionList = categoryManager.specialCategoryNames

                if (exemptionList.contains(taskListName)) {
                    dialogManager.showCategoriesDialog(
                        this@HomeFragment,
                        sharedViewModel,
                        BATCHADD,
                        null
                    ) { tasksAddedToCategory ->
                        if (tasksAddedToCategory) exitSelectionMode()
                    }
                } else {
                    dialogManager.showBatchRemoveTagConfirmationDialog(
                        this@HomeFragment,
                        sharedViewModel
                    ) {
                        if (it) exitSelectionMode()
                    }
                }
            }

            shareTasks.setOnClickListener {
                createShareTasksIntent()
            }

            // Observe current user data
            sharedViewModel.currentUser.observe(viewLifecycleOwner) { user ->
                if (user != null) {

                    // Set the user Email for feedback use
                    userEmail = user.email

                    // Set the nav drawer and home avatar imageViews
                    if (user.avatarFilePath != null) {
                        val avatarImageViews = arrayOf(avatarHome, avatarNavDrw)

                        val imageLoad = Glide.with(fragmentContext)
                            .load(File(user.avatarFilePath))
                            .listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    // Handle image loading failure
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    dataSource: DataSource?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    // Image has been successfully loaded, hide progress bar and overlay
                                    overlayView.visibility = View.GONE
                                    avatarProgressBar.visibility = View.GONE

                                    avatarProgressBarND.visibility = View.GONE
                                    overlayViewND.visibility = View.GONE
                                    return false
                                }
                            })

                        for (imageView in avatarImageViews) {
                            imageLoad.into(imageView)
                        }
                    }

                    // Set the nav drawer email
                    emailNavDrw.text = user.email
                    emailNavDrw.visibility = View.VISIBLE

                    // Set the nav drawer name
                    if (!user.name.isNullOrEmpty())
                        nameNavDrw.text = user.name

                    // Set the nav drawer occupation
                    if (!user.occupation.isNullOrEmpty())
                        occupationNavDrw.text = user.occupation
                }
            }


            // Observe all current user's tasks
            sharedViewModel.allTasksList.observe(viewLifecycleOwner) { allTasksList ->

                if (isSearchResultsShowing) {
                    searchView.setQuery(null, false)

                    isSearchResultsShowing = false
                    return@observe
                }

                when (checkTaskLists(allTasksList)) {
                    1 -> onEmptyLayout(1)
                    2 -> onEmptyLayout(2)
                    3 -> onEmptyLayout(3)
                }

                currentTasks = allTasksList

                taskListAdapter.updateTaskLists(allTasksList)
                parentRecyclerView.requestFocus()

                val uncompletedNumber =
                    allTasksList.find { it.name == UNCOMPLETED }?.list?.size ?: 0
                val completedNumber =
                    allTasksList.find { it.name == COMPLETED }?.list?.size ?: 0

                // Update uncompleted tasks text with string plurals template
                uncompletedTasks.text = resources.getQuantityString(
                    R.plurals.tasks_left,
                    uncompletedNumber,
                    uncompletedNumber
                )

                // Update completed tasks text with string plurals template
                completedTasks.text = resources.getQuantityString(
                    R.plurals.tasks_done,
                    completedNumber,
                    completedNumber
                )

                // Calculate the completion level
                val totalTasks = uncompletedNumber + completedNumber
                val completionLevel =
                    if (totalTasks > 0) uncompletedNumber.toFloat() / totalTasks else 0f

                // Set the width of the progress bar
                val progressBarWidth = (progressBackNavDrw.width * completionLevel).toInt()
                progressBarNavDrw.layoutParams.width = progressBarWidth
                progressBarNavDrw.requestLayout()

                // Check if progressBarWidth is 0 and completedNumber is greater than 0
                if (progressBarWidth == 0 && completedNumber > 0) {
                    // Set the background color of progressBackNavDrw to green
                    progressBackNavDrw.setBackgroundResource(R.drawable.rounded_background_progress_green)
                } else {
                    // Set the background color of progressBackNavDrw to default color
                    progressBackNavDrw.setBackgroundResource(R.drawable.rounded_background_progress)
                }

                // stop loading animation
                tasksProgressBar.visibility = View.GONE
            }

            // Observe task selection mode
            sharedViewModel.isSelectionModeOn.observe(viewLifecycleOwner) { isSelectionModeOn ->
                callback.isEnabled = isSelectionModeOn

                if (isSelectionModeOn) {
                    searchViewLayout.visibility = View.GONE
                    itemCountLayout.visibility = View.VISIBLE
                    separator.visibility = View.VISIBLE
                    fab.visibility = View.GONE
                    refactorTaskLayout.visibility = View.VISIBLE

                } else {
                    searchViewLayout.visibility = View.VISIBLE
                    itemCountLayout.visibility = View.GONE
                    separator.visibility = View.GONE
                    fab.visibility = View.VISIBLE
                    refactorTaskLayout.visibility = View.GONE
                }
            }

            // Observe highlighted tasks list
            sharedViewModel.highlightedTaskList.observe(viewLifecycleOwner) { highlightedTaskList ->

                if (highlightedTaskList.isNotEmpty()) {

                    val size = highlightedTaskList.size

                    // Update itemCounter tasks text with string plurals template
                    itemCounter.text = resources.getQuantityString(
                        R.plurals.tasks_selected,
                        size,
                        size
                    )

                    // Get the stroke color
                    val colorResourceId = if (isNightMode) {
                        R.color.myOnSurfaceNight // Use night mode color resource
                    } else {
                        R.color.myOnSurfaceDay // Use regular color resource
                    }

                    if (size > 1) {
                        renameSelected.isEnabled = false
                        editDateTime.isEnabled = false
                        renameSelected.setColorFilter(
                            ContextCompat.getColor(fragmentContext, R.color.my_darker_grey),
                            PorterDuff.Mode.SRC_IN
                        )
                        editDateTime.setColorFilter(
                            ContextCompat.getColor(fragmentContext, R.color.my_darker_grey),
                            PorterDuff.Mode.SRC_IN
                        )
                    } else {
                        renameSelected.isEnabled = true
                        editDateTime.isEnabled = true
                        renameSelected.setColorFilter(
                            ContextCompat.getColor(fragmentContext, colorResourceId),
                            PorterDuff.Mode.SRC_IN
                        )
                        editDateTime.setColorFilter(
                            ContextCompat.getColor(fragmentContext, colorResourceId),
                            PorterDuff.Mode.SRC_IN
                        )
                    }
                }
            }
        }
    }
    private fun createShareTasksIntent() {
        val taskList = sharedViewModel.highlightedTaskList.value
        val categoryList = sharedViewModel.categories.value
        val categoryMap = categoryList?.associateBy { it.categoryId }

        // Check if the taskList is not null and not empty
        if (!taskList.isNullOrEmpty()) {
            val dateFormat = SimpleDateFormat(
                "EEE, MMM dd, yyyy 'At' hh:mm a",
                Locale.getDefault()
            )

            val sharedText = buildString {
                append("My Tasks:\n\n")

                taskList.withIndex().forEach { (index, task) ->
                    append("${index + 1}. Title: ${task.title}\n")
                    if (task.description.isNotEmpty()) {
                        append("   Description: ${task.description}\n")
                    } else {
                        append("   Description: Not set\n")
                    }
                    append("   Completed: ${if (task.taskCompleted) "Yes" else "No"}\n")
                    append("   Starred: ${if (task.starred) "Yes" else "No"}\n")
                    if (task.dueDateTime != null) {
                        val formattedTime = task.dueDateTime.time.let { it1 ->
                            dateFormat.format(
                                it1
                            )
                        }
                        append("   Due Date & Time: $formattedTime\n")
                    } else {
                        append("   Due Date & Time: Not Set\n")
                    }

                    if (task.categoryIds.isNotEmpty()) {
                        val categoryNames = task.categoryIds.map { categoryId ->
                            categoryMap?.get(categoryId)?.categoryName ?: "None"
                        }
                        append("   Task Categories: ${categoryNames.joinToString(", ")}\n")
                    }
                    append("\n")
                }
            }

            // Now, you can use the `sharedText` to share the tasks using an Intent
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, sharedText)
            }

            startActivity(Intent.createChooser(sendIntent, "Share via"))
        } else {
            // Handle the case when the taskList is empty or null
            toastManager.showShortToast(requireContext(), "No tasks to share")
        }
    }

    private fun registerEvents() {
        binding.apply {

            // Set up listeners
            avatarHome.setOnClickListener {
                toggleNavigationDrawer()
            }

            fab.setOnClickListener {
                bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
            }

            binding.moreOptions.setOnClickListener { view ->
                showOverflowMenu(view)
            }
        }
    }

    fun upDateAdapterWithFilteredTasks() {
        val filteredTaskList = sharedViewModel.filteredTaskList.value ?: emptyList()

        currentTasks = filteredTaskList

        taskListAdapter.updateTaskLists(filteredTaskList)

    }

    private fun updateFromDatabase() {
        sharedViewModel.startDatabaseListeners()
        stopRefreshing()
    }

    private fun stopRefreshing() {
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun checkTaskLists(newList: List<TaskList>): Int {
        val isEmpty = newList.all { it.list.isEmpty() }
        val uncompletedListIsEmpty = newList.any { it.name == UNCOMPLETED && it.list.isEmpty() }

        return when {
            isEmpty -> 1
            uncompletedListIsEmpty -> 2
            else -> 3
        }
    }

    private fun onEmptyLayout(isEmpty: Int) {
        binding.apply {
            when (isEmpty) {
                1 -> {
                    // No tasks in list
                    emptyTaskTitle.text = getString(R.string.no_tasks_yet)
                    emptyTaskBody.text = getString(R.string.add_tasks)
                    emptinessLayout.visibility = View.VISIBLE
                    emptyTaskImage.visibility = View.VISIBLE
                    emptyTaskTitle.visibility = View.VISIBLE

                    searchViewLayout.visibility = View.INVISIBLE
                    parentRecyclerView.visibility = View.INVISIBLE
                }

                2 -> {
                    // All tasks completed
                    emptyTaskTitle.text = getString(R.string.tasks_completed)
                    emptyTaskBody.text = getString(R.string.well_done)
                    emptinessLayout.visibility = View.VISIBLE
                    emptyTaskImage.visibility = View.VISIBLE
                    emptyTaskTitle.visibility = View.VISIBLE

                    searchViewLayout.visibility = View.INVISIBLE
                    parentRecyclerView.visibility = View.INVISIBLE
                }

                3 -> {
                    // List contains tasks
                    emptinessLayout.visibility = View.INVISIBLE
                    searchViewLayout.visibility = View.VISIBLE
                    parentRecyclerView.visibility = View.VISIBLE
                }

                4 -> {
                    // FilteredList empty
                    emptyTaskImage.visibility = View.GONE
                    emptyTaskTitle.visibility = View.GONE
                    emptyTaskBody.text = getString(R.string.no_tasks_here)

                    emptinessLayout.visibility = View.VISIBLE
                    searchViewLayout.visibility = View.INVISIBLE
                    parentRecyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun toggleNavigationDrawer() {
        // Toggle the navigation drawer open or close
        if (navDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            navDrawerLayout.closeDrawer(GravityCompat.START)
        } else {
            navDrawerLayout.requestFocus()
            navDrawerLayout.openDrawer(GravityCompat.START)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.settings -> {
                navDrawerLayout.closeDrawer(GravityCompat.START)
                findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
            }

            R.id.account -> {
                navDrawerLayout.closeDrawer(GravityCompat.START)
                findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
            }

            R.id.supportUs -> {
                navDrawerLayout.closeDrawer(GravityCompat.START)
                findNavController().navigate(R.id.action_homeFragment_to_supportUsFragment)
            }

            R.id.feedback -> {
                userEmail?.let { dialogManager.showFeedbackDialog(this, it) }

                navDrawerLayout.closeDrawer(GravityCompat.START)
            }

            R.id.logInOut -> {
                // close navigation drawer
                navDrawerLayout.closeDrawer(GravityCompat.START)

                // Show confirm dialog
                dialogManager.showLogoutConfirmationDialog(
                    this,
                    sharedViewModel
                ) { isSuccessful ->
                    if (isSuccessful) {
                        // Navigate to SignInFragment
                        findNavController().navigate(R.id.action_homeFragment_to_signInFragment)
                    }
                }
            }
        }
        return true
    }

    private fun getStarredList() {
        sharedViewModel.filterTasks(null, STAR) {
            if (it) upDateAdapterWithFilteredTasks()
        }
    }

    private fun exitSelectionMode() {
        sharedViewModel.setIsSelectionModeOn(false)
        sharedViewModel.updateHighlightedListName("")
    }

    private fun showOverflowMenu(view: View) {
        val popupMenu = PopupMenu(fragmentContext, view)

        // Inflate the menu resource
        popupMenu.menuInflater.inflate(R.menu.home_overflow_menu, popupMenu.menu)

        // Set an OnMenuItemClickListener for the menu items
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_favorites -> {
                    if (isSearchResultsShowing) {
                        toastManager.showLongToast(
                            fragmentContext,
                            "Clear the search bar to get complete results"
                        )
                    }

                    getStarredList()

                    true
                }

                R.id.menu_sort -> {
                    dialogManager.showSortingDialog(
                        this,
                        sharedViewModel,
                        currentTasks
                    )
                    true
                }

                R.id.menu_category -> {
                    dialogManager.showCategoriesDialog(
                        this,
                        sharedViewModel,
                        TAG,
                        null
                    ) {
                        if (it) upDateAdapterWithFilteredTasks()
                    }
                    true
                }

                R.id.menu_batch -> {
                    findNavController().navigate(R.id.action_homeFragment_to_batchCreateTasksFragment)
                    true
                }

                R.id.menu_view -> {

                    // TODO Calender View
                    toastManager.showShortToast(requireContext(),"Calender View is coming soon!")
                    true
                }

                else -> false
            }
        }

        // Show the popup menu
        popupMenu.show()
    }

    private fun showOnboardingPrompts() {
        binding.apply {
            // Switch visibility of views for onboarding
            topBarOverlay.visibility = View.VISIBLE
            swipeRefreshLayoutOverlay.visibility = View.VISIBLE
            fabOverlay.visibility = View.VISIBLE

            topBar.visibility = View.INVISIBLE
            swipeRefreshLayout.visibility = View.INVISIBLE
            fab.visibility = View.INVISIBLE

            val tapTarget1 = MaterialTapTargetPrompt.Builder(requireActivity())
                .setTarget(fabOverlay)
                .setPrimaryText("Welcome to TodoMobile!")
                .setSecondaryText("Tap here to create a new task.")
                .setAutoDismiss(false)
                .setBackgroundColour(ContextCompat.getColor(requireContext(), R.color.myPrimary))

            val tapTarget2 = MaterialTapTargetPrompt.Builder(requireActivity())
                .setTarget(avatarHomeOverlay)
                .setPrimaryText("Profile Image")
                .setSecondaryText("Tap here or swipe right from the edge of the screen to access the navigation drawer.")
                .setAutoDismiss(false)
                .setBackgroundColour(ContextCompat.getColor(requireContext(), R.color.myPrimary))

            val tapTarget3 = MaterialTapTargetPrompt.Builder(requireActivity())
                .setTarget(moreOptionsOverlay)
                .setPrimaryText("Menu options")
                .setSecondaryText("Access more features here")
                .setAutoDismiss(false)
                .setAutoFinish(true)
                .setBackgroundColour(ContextCompat.getColor(requireContext(), R.color.myPrimary))

            tapTarget1.setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED) {
                    tapTarget2.show()
                }
            }.show()

            tapTarget2.setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED) {
                    tapTarget3.show()
                }
            }

            // Set an event listener for when the prompt is completed
            tapTarget3.setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_FINISHED) {
                    // Reset onboarding views
                    topBar.visibility = View.VISIBLE
                    swipeRefreshLayout.visibility = View.VISIBLE
                    fab.visibility = View.VISIBLE

                    topBarOverlay.visibility = View.GONE
                    swipeRefreshLayoutOverlay.visibility = View.GONE
                    fabOverlay.visibility = View.GONE

                    // Update the firstLaunchStatus in the viewModel
                    sharedViewModel.updateFirstLaunchStatus(false)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the state of flags
        outState.putBoolean("isSearchResultsShowing", isSearchResultsShowing)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // Restore the state of flags
        isSearchResultsShowing =
            savedInstanceState?.getBoolean("isSearchResultsShowing") ?: false
    }

    companion object {
        private const val TAG = "HomeFragment"
        private const val STAR = "Favorites"
        private const val SEARCH = "Search Results"
        private const val UNCOMPLETED = "uncompleted"
        private const val COMPLETED = "completed"
        private const val BATCHADD = "BatchAdd"
    }

}