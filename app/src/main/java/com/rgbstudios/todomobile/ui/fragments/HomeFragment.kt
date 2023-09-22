package com.rgbstudios.todomobile.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener
import com.google.android.material.textfield.TextInputEditText
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.TodoMobileApplication
import com.rgbstudios.todomobile.databinding.FragmentHomeBinding
import com.rgbstudios.todomobile.model.TaskList
import com.rgbstudios.todomobile.ui.adapters.ListAdapter
import com.rgbstudios.todomobile.utils.DialogManager
import com.rgbstudios.todomobile.utils.ToastManager
import com.rgbstudios.todomobile.viewmodel.TodoViewModel
import com.rgbstudios.todomobile.viewmodel.TodoViewModelFactory
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import java.io.File


class HomeFragment : Fragment(),
    OnNavigationItemSelectedListener {

    private val sharedViewModel: TodoViewModel by activityViewModels {
        TodoViewModelFactory(activity?.application as TodoMobileApplication)
    }

    private lateinit var binding: FragmentHomeBinding
    private lateinit var bottomSheetFragment: BottomSheetFragment
    private val listener = this as OnNavigationItemSelectedListener
    private lateinit var taskListAdapter: ListAdapter
    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var navDrawerLayout: DrawerLayout
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var fragmentContext: Context
    private val dialogManager = DialogManager()
    private val toastManager = ToastManager()
    private var isSearchResultsShowing = false

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

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getPreferences(Context.MODE_PRIVATE)

        // Check if it's the first launch
        val isFirstLaunch = sharedPreferences.getBoolean("first_launch", true)

        if (isFirstLaunch) {
            // Show onboarding prompts
            showOnboardingPrompts()
        }

        init()
        updateFromDatabase()
        registerEvents()
    }

    private fun init() {

        // Set up the adapter
        taskListAdapter = ListAdapter(fragmentContext, sharedViewModel)

        binding.apply {

            // Set up the recyclerView
            parentRecyclerView.setHasFixedSize(true)
            parentRecyclerView.layoutManager = LinearLayoutManager(context)
            parentRecyclerView.adapter = taskListAdapter

            // Set up the nav drawer
            navDrawerLayout = drawerLayout
            navigationView.setNavigationItemSelectedListener(listener)

            refreshLayout = swipeRefreshLayout
            swipeRefreshLayout.setOnRefreshListener {
                // Update data from database when the user performs the pull-to-refresh action
                updateFromDatabase()
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

                        sharedViewModel.filterTasks(query, SEARCH)
                        true
                    }
                    return true
                }
            })

            // Observe LiveData
            observeLiveData()
        }
    }

    private fun observeLiveData() {
        binding.apply {

            val navigationDrawerView = navigationView
            // Get the header view from NavigationView
            val headerView = navigationDrawerView.getHeaderView(0)

            val avatarNavDrw = headerView.findViewById<ImageView>(R.id.avatarNavDrw)
            val nameNavDrw = headerView.findViewById<TextView>(R.id.userNameTxt)
            val emailNavDrw = headerView.findViewById<TextView>(R.id.emailTxt)
            val occupationNavDrw = headerView.findViewById<TextView>(R.id.occupationTxt)
            val uncompletedTasks = headerView.findViewById<TextView>(R.id.unCompletedTasksNumber)
            val completedTasks = headerView.findViewById<TextView>(R.id.completedTasksNumber)
            val progressBarNavDrw = headerView.findViewById<View>(R.id.progressBar)
            val progressBackNavDrw = headerView.findViewById<View>(R.id.progressBackground)

            // Set up OnClickListener for the navigation drawer header
            headerView.setOnClickListener {
                navDrawerLayout.closeDrawer(GravityCompat.START)
                findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
            }

            // Observe current user data
            sharedViewModel.currentUser.observe(viewLifecycleOwner) { user ->
                if (user != null) {

                    // Set the nav drawer and home avatar imageViews
                    if (user.avatarFilePath != null) {
                        val avatarImageViews =
                            arrayOf(avatarHome, avatarNavDrw)

                        val imageLoad = Glide.with(fragmentContext).load(File(user.avatarFilePath))

                        for (imageView in avatarImageViews) {
                            imageLoad.circleCrop().into(imageView)
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
                when (checkTaskLists(allTasksList)) {
                    1 -> onEmptyLayout(1)
                    2 -> onEmptyLayout(2)
                    3 -> onEmptyLayout(3)
                }
                taskListAdapter.updateTaskLists(allTasksList)

                val uncompletedNumber =
                    allTasksList.find { it.name == UNCOMPLETED }?.list?.size ?: 0
                val completedNumber = allTasksList.find { it.name == COMPLETED }?.list?.size ?: 0

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
                val progressBarWidth = (headerView.width * completionLevel).toInt()
                progressBarNavDrw.layoutParams.width = progressBarWidth
                progressBarNavDrw.requestLayout()

                // Check if progressBarWidth is 0 and completedNumber is greater than 0
                if (progressBarWidth == 0 && completedNumber > 0) {
                    // Set the background color of progressBackNavDrw to green
                    progressBackNavDrw.setBackgroundColor(
                        ContextCompat.getColor(
                            fragmentContext,
                            R.color.myGreen
                        )
                    )
                } else {
                    // Set the background color of progressBackNavDrw to default color
                    progressBackNavDrw.setBackgroundColor(
                        ContextCompat.getColor(
                            fragmentContext,
                            android.R.color.darker_gray
                        )
                    )
                }

                isSearchResultsShowing = false
            }

            sharedViewModel.filteredTaskList.observe(viewLifecycleOwner) { filteredTaskList ->
                if (checkTaskLists(filteredTaskList) == 1) {
                    onEmptyLayout(4)
                } else {
                    onEmptyLayout(3)
                }
                taskListAdapter.updateTaskLists(filteredTaskList)
            }
        }
    }

    private fun registerEvents() {
        binding.apply {

            // Set up listeners
            avatarHome.setOnClickListener {
                toggleNavigationDrawer()
            }

            fab.setOnClickListener {
                bottomSheetFragment = BottomSheetFragment()
                bottomSheetFragment.setListener(
                    object : BottomSheetFragment.AddTaskBtnClickListener {
                        override fun onSaveTask(
                            title: String,
                            description: String,
                            titleEt: TextInputEditText,
                            descriptionEt: TextInputEditText,
                            starred: Boolean
                        ) {
                            // Call the ViewModel's method to save the task
                            sharedViewModel.saveTask(
                                title,
                                description,
                                starred
                            ) { isSuccessful ->
                                if (isSuccessful) {
                                    // Handle success
                                    toastManager.showToast(
                                        fragmentContext,
                                        "Task saved successfully!"
                                    )

                                    // Clear the EditText fields
                                    titleEt.text = null
                                    descriptionEt.text = null
                                    emptinessLayout.visibility = View.INVISIBLE
                                } else {
                                    // Handle failure
                                    toastManager.showToast(
                                        fragmentContext,
                                        "Failed to save task"
                                    )
                                }

                                bottomSheetFragment.dismiss()

                                // Set focus to the recyclerview to prevent the searchView from triggering the keyboard
                                parentRecyclerView.requestFocus()
                            }
                        }

                    }
                )
                bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
            }

            binding.moreOptions.setOnClickListener { view ->
                showOverflowMenu(view)
            }
        }
    }

    private fun updateFromDatabase() {
        sharedViewModel.startDatabaseListeners()
        stopRefreshing()
    }

    private fun stopRefreshing() {
        refreshLayout.isRefreshing = false
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
            navDrawerLayout.openDrawer(GravityCompat.START)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.appSettings -> {
                findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
            }

            R.id.account -> {
                findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
            }

            R.id.supportUs -> {
                // findNavController().navigate(R.id.action_homeFragment_to_aboutUsFragment)
            }

            R.id.feedback -> {
                dialogManager.showFeedbackDialog()
            }

            R.id.logInOut -> {
                // close navigation drawer
                navDrawerLayout.closeDrawer(GravityCompat.START)

                // Show confirm dialog
                dialogManager.showLogoutConfirmationDialog(this, sharedViewModel) { isSuccessful ->
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
        sharedViewModel.filterTasks(null, STAR)
    }

    private fun sortCurrentList() {
        //TODO
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
                        toastManager.showToast(
                            fragmentContext,
                            "Clear the search bar to get complete results"
                        )
                    }

                    getStarredList()

                    true
                }

                R.id.menu_sort -> {
                    true
                }

                R.id.menu_category -> {
                    dialogManager.showCategoriesDialog(
                        this,
                        sharedViewModel,
                        TAG,
                        null
                    )
                    true
                }

                R.id.menu_batch -> {
                    true
                }

                R.id.menu_view -> {
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

            val tapTarget2 = MaterialTapTargetPrompt.Builder(requireActivity())
                .setTarget(avatarHomeOverlay)
                .setPrimaryText("Profile Image")
                .setSecondaryText("Tap here or swipe right from the edge of the screen to access the navigation drawer.")
                .setAutoDismiss(false)

            val tapTarget3 = MaterialTapTargetPrompt.Builder(requireActivity())
                .setTarget(moreOptionsOverlay)
                .setPrimaryText("Menu options")
                .setSecondaryText("Access more features here")
                .setAutoDismiss(false)
                .setAutoFinish(true)

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

                    // Mark that onboarding has been completed
                    with(sharedPreferences.edit()) {
                        putBoolean("first_launch", false)
                        apply()
                    }
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
        isSearchResultsShowing = savedInstanceState?.getBoolean("isSearchResultsShowing") ?: false
    }

    companion object {
        private const val TAG = "HomeFragment"
        private const val STAR = "Favorites"
        private const val SEARCH = "Search Results"
        private const val UNCOMPLETED = "uncompleted"
        private const val COMPLETED = "completed"
    }

}