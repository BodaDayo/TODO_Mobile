package com.rgbstudios.todomobile

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.SearchView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.rgbstudios.todomobile.adapter.ListAdapter
import com.rgbstudios.todomobile.databinding.FragmentHomeBinding
import com.rgbstudios.todomobile.model.TaskViewModel


class HomeFragment : Fragment(), BottomSheetFragment.DialogAddTaskBtnClickListener,
    NavigationView.OnNavigationItemSelectedListener {

    private val sharedViewModel: TaskViewModel by activityViewModels()

    private lateinit var binding: FragmentHomeBinding
    private lateinit var bottomSheetFragment: BottomSheetFragment
    private lateinit var adapter: ListAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var drawerLayout: DrawerLayout

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
        getDataFromFirebase()
        registerEvents()
    }

    private fun init() {
        resetCurrentList()

        binding.parentRecyclerView.setHasFixedSize(true)
        binding.parentRecyclerView.layoutManager = LinearLayoutManager(context)

        adapter = ListAdapter(sharedViewModel)

        binding.parentRecyclerView.adapter = adapter

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                sharedViewModel.filterTasks(p0)
                return true
            }
        })

        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.starredList -> {
                    getStarredList()
                }

                R.id.sort -> {
                    sortCurrentList()
                }

                R.id.focus -> {
                    //TODO Navigate to focus fragment
                }

                R.id.profile -> {
                    toggleNavigationDrawer()
                }
            }
            true
        }
        binding.bottomNavigationView.selectedItemId = 0

        drawerLayout = binding.drawerLayout
        binding.toggleNav.setOnClickListener { toggleNavigationDrawer() }

        binding.navigationView.setNavigationItemSelectedListener(this)

        // Observe the isUserSignedIn LiveData from the sharedViewModel
        sharedViewModel.isUserSignedIn.observe(viewLifecycleOwner) { isUserSignedIn ->
            // Reference to the logOut menu item
            val logOutMenuItem = binding.navigationView.menu.findItem(R.id.logInOut)

            if (isUserSignedIn) {
                // Update menu item title and icon when user is signed in
                logOutMenuItem.title = "Log Out"
                logOutMenuItem.setIcon(R.drawable.logout)
            } else {
                // Update menu item title and icon when user is not signed in
                logOutMenuItem.title = "Sign In"
                logOutMenuItem.setIcon(R.drawable.login)
                //TODO set display icon
            }
        }

        // Observe the allTasksList from the sharedViewModel and update the adapter
        sharedViewModel.allTasksList.observe(viewLifecycleOwner) { allTasksList ->
            adapter.updateTaskLists(allTasksList)
            onEmptyLayout(allTasksList?.isEmpty() ?: true)
        }

        // Observe the filteredTaskList from the sharedViewModel and update the adapter for searchQuery
        sharedViewModel.filteredTaskList.observe(viewLifecycleOwner) { filteredTaskList ->
            adapter.updateTaskLists(filteredTaskList)
        }

        swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            // This code will be triggered when the user performs the pull-to-refresh action
            getDataFromFirebase()
        }

    }

    private fun registerEvents() {
        binding.fab.setOnClickListener {

            if (sharedViewModel.isUserSignedIn.value == true) {
                bottomSheetFragment = BottomSheetFragment()
                bottomSheetFragment.setListener(this)
                bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
                Toast.makeText(context, "Sign in to continue\nOffline mode coming soon!", Toast.LENGTH_SHORT).show()
            }

        }
    }

    override fun onSaveTask(
        title: String,
        description: String,
        titleEt: TextInputEditText,
        descriptionEt: TextInputEditText
    ) {

        // Call the ViewModel's method to save the task
        sharedViewModel.saveTask(title, description) { isSuccessful ->
            if (isSuccessful) {
                // Handle success
                Toast.makeText(context, "Task saved successfully!", Toast.LENGTH_SHORT).show()
                // Clear the EditText fields
                titleEt.text = null
                descriptionEt.text = null
                binding.emptinessLayout.visibility = View.GONE
            } else {
                // Handle failure
                Toast.makeText(context, "Failed to save task", Toast.LENGTH_SHORT).show()
            }

            bottomSheetFragment.dismiss()

            // Set focus to the recyclerview to prevent the searchView from triggering the keyboard
            binding.parentRecyclerView.requestFocus()
        }

    }

    private fun getDataFromFirebase() {
        sharedViewModel.getTasksFromFirebase()

        /* TODO Get condition from vieModel getTasksFromFirebase
        fun onCancelled(error: DatabaseError) {
            //Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
        }*/
        stopRefreshing()
    }

    // This function will be called when you want to stop the refreshing animation
    private fun stopRefreshing() {
        swipeRefreshLayout.isRefreshing = false
    }

    private fun onEmptyLayout(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptinessLayout.visibility = View.VISIBLE
            binding.searchViewLayout.visibility = View.GONE
        } else {
            binding.emptinessLayout.visibility = View.GONE
            binding.searchViewLayout.visibility = View.VISIBLE
        }
    }

    private fun resetCurrentList() {
        sharedViewModel.setupAuthStateListener()
        sharedViewModel.resetList() // Clear the RecyclerView
        onEmptyLayout(true) // Show the empty layout
    }

    private fun getStarredList() {
        //TODO
    }

    private fun sortCurrentList() {
        //TODO
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBackPressedMethod()
        }
    }

    private fun onBackPressedMethod() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.isDrawerOpen(GravityCompat.START)
        } else {
            requireActivity().finish()
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

            }

            R.id.profile -> {

            }

            R.id.logInOut -> {
                if (item.title == "Log Out" ) {
                    showLogoutConfirmationDialog()
                } else {
                    findNavController().navigate(R.id.action_homeFragment_to_signInFragment)
                }
            }
        }
        return true
    }

    private fun showLogoutConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_logout_confirmation, null)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        val btnLogoutConfirm = dialogView.findViewById<Button>(R.id.btnLogoutConfirm)
        val btnLogoutCancel = dialogView.findViewById<Button>(R.id.btnLogoutCancel)

        btnLogoutConfirm.setOnClickListener {
            // Call the ViewModel's logout method to sign out the user
            sharedViewModel.logout()

            // Dismiss the dialog
            dialog.dismiss()

            // close drawer
            drawerLayout.closeDrawer(GravityCompat.START)

            resetCurrentList()
        }

        btnLogoutCancel.setOnClickListener {
            // Dismiss the dialog
            dialog.dismiss()
        }

        dialog.show()
    }
}