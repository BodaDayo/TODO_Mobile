package com.rgbstudios.todomobile.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.analytics.FirebaseAnalytics.Event.SEARCH
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.TodoMobileApplication
import com.rgbstudios.todomobile.databinding.DialogRemoveConfirmationBinding
import com.rgbstudios.todomobile.databinding.FragmentHomeBinding
import com.rgbstudios.todomobile.model.TaskList
import com.rgbstudios.todomobile.ui.adapters.ListAdapter
import com.rgbstudios.todomobile.viewmodel.TodoViewModel
import com.rgbstudios.todomobile.viewmodel.TodoViewModelFactory
import java.io.File
import kotlin.reflect.KTypeProjection.Companion.STAR

class HomeFragment : Fragment(), BottomSheetFragment.DialogAddTaskBtnClickListener,
    NavigationView.OnNavigationItemSelectedListener {

    private val sharedViewModel: TodoViewModel by activityViewModels {
        TodoViewModelFactory(activity?.application as TodoMobileApplication)
    }

    private lateinit var binding: FragmentHomeBinding
    private lateinit var bottomSheetFragment: BottomSheetFragment
    private lateinit var adapter: ListAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var drawerLayout: DrawerLayout
    private var isStarredListShowing = false
    private var isSearchResultsShowing = false
    private var isCurrentUserInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        updateFromDatabase()
        registerEvents()
    }

    private fun init() {

        binding.parentRecyclerView.setHasFixedSize(true)
        binding.parentRecyclerView.layoutManager = LinearLayoutManager(context)

        adapter = ListAdapter(requireContext(), sharedViewModel)

        binding.parentRecyclerView.adapter = adapter

        drawerLayout = binding.drawerLayout

        binding.avatarHome.setOnClickListener { toggleNavigationDrawer() }

        binding.navigationView.setNavigationItemSelectedListener(this)

        swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            // This code will be triggered when the user performs the pull-to-refresh action
            updateFromDatabase()
        }

        // Set up SearchView's query text listener
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(query: String?): Boolean {
                isSearchResultsShowing = if (query.isNullOrEmpty()) {
                    sharedViewModel.startTasksListener()
                    false
                } else {
                    sharedViewModel.filterTasks(query, SEARCH)
                    true
                }
                return true
            }
        })

        // Set up OnClickListener for the "moreOptions" ImageView
        binding.moreOptions.setOnClickListener { view ->
            showOverflowMenu(view)
        }

        // Observe LiveData
        observeLiveData()
    }

    private fun observeLiveData() {
        val navigationView = requireView().findViewById<NavigationView>(R.id.navigationView)
        val headerView = navigationView.getHeaderView(0) // Get the header view from NavigationView

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
            drawerLayout.closeDrawer(GravityCompat.START)
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }

        sharedViewModel.currentUser.observe(viewLifecycleOwner) { user ->

            if (user != null) {

                isCurrentUserInitialized = true

                // Set the nav drawer and home avatar imageViews
                if (user.avatarFilePath != null) {
                    val avatarImageViews =
                        arrayOf(binding.avatarHome, avatarNavDrw)

                    val imageLoad = Glide.with(requireContext()).load(File(user.avatarFilePath))

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

        sharedViewModel.allTasksList.observe(viewLifecycleOwner) { allTasksList ->
            when (checkTaskLists(allTasksList)) {
                1 -> onEmptyLayout(1)
                2 -> onEmptyLayout(2)
                3 -> onEmptyLayout(3)
            }
            adapter.updateTaskLists(allTasksList)

            val uncompletedNumber = allTasksList.find { it.name == "uncompleted" }?.list?.size ?: 0
            val completedNumber = allTasksList.find { it.name == "completed" }?.list?.size ?: 0

            // Update uncompleted tasks text
            uncompletedTasks.text = resources.getQuantityString(
                R.plurals.tasks_left,
                uncompletedNumber,
                uncompletedNumber
            )

            // Update completed tasks text
            completedTasks.text = resources.getQuantityString(
                R.plurals.tasks_done,
                completedNumber,
                completedNumber
            )

            // Calculate the completion level
            val totalTasks = uncompletedNumber + completedNumber
            val completionLevel = if (totalTasks > 0) uncompletedNumber.toFloat() / totalTasks else 0f

            // Set the width of the progress bar
            val progressBarWidth = (headerView.width * completionLevel).toInt()
            progressBarNavDrw.layoutParams.width = progressBarWidth
            progressBarNavDrw.requestLayout()

            // Check if progressBarWidth is 0 and completedNumber is greater than 0
            if (progressBarWidth == 0 && completedNumber > 0) {
                // Set the background color of progressBackNavDrw to R.color.myGreen
                progressBackNavDrw.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.myGreen))
            } else {
                // Set the background color of progressBackNavDrw to your default color
                progressBackNavDrw.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            }

            isStarredListShowing = false
            isSearchResultsShowing = false
        }

        sharedViewModel.filteredTaskList.observe(viewLifecycleOwner) { filteredTaskList ->
            adapter.updateTaskLists(filteredTaskList)
        }
    }

    private fun registerEvents() {
        binding.fab.setOnClickListener {
            bottomSheetFragment = BottomSheetFragment()
            bottomSheetFragment.setListener(this)
            bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)

        }
    }

    override fun onSaveTask(
        title: String,
        description: String,
        titleEt: TextInputEditText,
        descriptionEt: TextInputEditText,
        starred: Boolean
    ) {

        // Call the ViewModel's method to save the task
        sharedViewModel.saveTask(title, description, starred) { isSuccessful ->
            if (isSuccessful) {
                // Handle success
                Toast.makeText(context, "Task saved successfully!", Toast.LENGTH_SHORT).show()

                // Clear the EditText fields
                titleEt.text = null
                descriptionEt.text = null
                binding.emptinessLayout.visibility = View.INVISIBLE
            } else {
                // Handle failure
                Toast.makeText(context, "Failed to save task", Toast.LENGTH_SHORT).show()
            }

            bottomSheetFragment.dismiss()

            // Set focus to the recyclerview to prevent the searchView from triggering the keyboard
            binding.parentRecyclerView.requestFocus()
        }
    }

    private fun updateFromDatabase() {
        sharedViewModel.startUserListener()
        sharedViewModel.startTasksListener()
        stopRefreshing()
    }

    private fun stopRefreshing() {
        swipeRefreshLayout.isRefreshing = false
    }

    private fun checkTaskLists(newList: List<TaskList>): Int {
        val isEmpty = newList.all { it.list.isEmpty() }
        val uncompletedListIsEmpty = newList.any { it.name == "uncompleted" && it.list.isEmpty() }

        return when {
            isEmpty -> 1
            uncompletedListIsEmpty -> 2
            else -> 3
        }
    }

    private fun onEmptyLayout(isEmpty: Int) {
        when (isEmpty) {
            1 -> {
                binding.emptyTaskTitle.text = getString(R.string.no_tasks_yet)
                binding.emptyTaskBody.text = getString(R.string.add_tasks)
                binding.emptinessLayout.visibility = View.VISIBLE
                binding.searchViewLayout.visibility = View.INVISIBLE
                binding.parentRecyclerView.visibility = View.INVISIBLE
            }

            2 -> {
                binding.emptyTaskTitle.text = getString(R.string.tasks_completed)
                binding.emptyTaskBody.text = getString(R.string.well_done)
                binding.emptinessLayout.visibility = View.VISIBLE
                binding.searchViewLayout.visibility = View.INVISIBLE
                binding.parentRecyclerView.visibility = View.INVISIBLE
            }

            3 -> {
                binding.emptinessLayout.visibility = View.INVISIBLE
                binding.searchViewLayout.visibility = View.VISIBLE
                binding.parentRecyclerView.visibility = View.VISIBLE
            }
        }

    }

    private fun toggleNavigationDrawer() {
        // Toggle the navigation drawer open or close
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.appSettings -> {
                // TODO Settings Fragment
            }

            R.id.profile -> {
                findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
            }

            R.id.logInOut -> {
                if (item.title == "Log Out") {
                    // close drawer
                    drawerLayout.closeDrawer(GravityCompat.START)

                    // Show confirm dialog
                    showLogoutConfirmationDialog()
                } else {
                    findNavController().navigate(R.id.action_homeFragment_to_signInFragment)
                }
            }
        }
        return true
    }

    private fun showLogoutConfirmationDialog() {
        val dialogBinding = DialogRemoveConfirmationBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnConfirm.setOnClickListener {
            // Call the ViewModel's logout method to sign out the user
            sharedViewModel.logOut { logOutSuccessful, errorMessage ->

                if (logOutSuccessful) {
                    // Dismiss the dialog
                    dialog.dismiss()

                    // Navigate to SignInFragment
                    findNavController().navigate(R.id.action_homeFragment_to_signInFragment)

                } else {
                    // Dismiss the dialog
                    dialog.dismiss()

                    errorMessage?.let { message ->
                        val output = message.substringAfter(": ")
                        Toast.makeText(context, output, Toast.LENGTH_SHORT).show()
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

    private fun getStarredList() {
        sharedViewModel.filterTasks(null, STAR)
    }

    private fun sortCurrentList() {
        //TODO
    }

    private fun showOverflowMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)

        // Inflate the menu resource
        popupMenu.menuInflater.inflate(R.menu.home_overflow_menu, popupMenu.menu)

        // Set an OnMenuItemClickListener for the menu items
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_favorites -> {
                    if (isSearchResultsShowing) {
                        Toast.makeText(
                            requireContext(),
                            "Clear the search bar to get complete results",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    getStarredList()

                    isStarredListShowing = true
                    true
                }

                R.id.menu_sort -> {
                    true
                }

                R.id.menu_category -> {
                    true
                }

                R.id.menu_focus -> {
                    true
                }

                else -> false
            }
        }

        // Show the popup menu
        popupMenu.show()
    }

    companion object {
        private const val TAG = "HomeFragment"
        private const val STAR = "Favorites"
        private const val SEARCH = "Search Results"
    }
}