package com.rgbstudios.todomobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
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
import com.google.android.material.textfield.TextInputEditText
import com.rgbstudios.todomobile.adapter.ListAdapter
import com.rgbstudios.todomobile.data.Avatars
import com.rgbstudios.todomobile.databinding.DialogLogoutConfirmationBinding
import com.rgbstudios.todomobile.databinding.FragmentHomeBinding
import com.rgbstudios.todomobile.databinding.NavHeaderBinding
import com.rgbstudios.todomobile.model.TaskViewModel


class HomeFragment : Fragment(), BottomSheetFragment.DialogAddTaskBtnClickListener,
    NavigationView.OnNavigationItemSelectedListener {

    private val sharedViewModel: TaskViewModel by activityViewModels()

    private lateinit var binding: FragmentHomeBinding
    private lateinit var bottomSheetFragment: BottomSheetFragment
    private lateinit var adapter: ListAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var drawerLayout: DrawerLayout

    // Get the drawable resource ID of a random avatar
    private val avatars = Avatars()
    private val avatarResource = avatars.getAvatar()

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

        val navigationView = requireView().findViewById<NavigationView>(R.id.navigationView)
        val headerView = navigationView.getHeaderView(0) // Get the header view from NavigationView

        val avatarNavDrw = headerView.findViewById<ImageView>(R.id.avatarNavDrw)
        val nameNavDrw = headerView.findViewById<TextView>(R.id.userNameTxt)
        val emailNavDrw = headerView.findViewById<TextView>(R.id.emailTxt)
        val occupationNavDrw = headerView.findViewById<TextView>(R.id.occupationTxt)

        binding.parentRecyclerView.setHasFixedSize(true)
        binding.parentRecyclerView.layoutManager = LinearLayoutManager(context)

        adapter = ListAdapter(sharedViewModel)

        binding.parentRecyclerView.adapter = adapter

        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.starredList -> {
                    getStarredList()
                }

                R.id.sort -> {
                    sortCurrentList()
                }

                R.id.focus -> {
                    // TODO Navigate to focus fragment
                }

                R.id.profile -> {
                    findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
                }
            }
            true
        }
        binding.bottomNavigationView.selectedItemId = 0

        drawerLayout = binding.drawerLayout
        binding.toggleNav.setOnClickListener { toggleNavigationDrawer() }

        binding.navigationView.setNavigationItemSelectedListener(this)

        swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            // This code will be triggered when the user performs the pull-to-refresh action
            getDataFromFirebase()
        }

        // Observe userAvatarUrl LiveData
        sharedViewModel.userAvatarUrl.observe(viewLifecycleOwner) { avatarUrl ->
            val avatarImageViews =
                arrayOf(binding.avatarHome, avatarNavDrw)

            val imageLoad = if (avatarUrl != null) {
                Glide.with(requireContext()).load(avatarUrl)
            } else {
                Glide.with(requireContext()).load(avatarResource)
            }

            for (imageView in avatarImageViews) {
                imageLoad.circleCrop().into(imageView)
            }
        }

        // Observe userEmail LiveData
        sharedViewModel.userEmail.observe(viewLifecycleOwner) {
            emailNavDrw.text = it
            emailNavDrw.visibility = View.VISIBLE
        }

        // Observe userDetails LiveData
        sharedViewModel.userDetailsFromFirebase.observe(viewLifecycleOwner) {
            nameNavDrw.text = it.name
            occupationNavDrw.text = it.occupation
        }

        // Observe isUserSignedIn LiveData
        sharedViewModel.isUserSignedIn.observe(viewLifecycleOwner) { isUserSignedIn ->
            // Update menu item title and icon based on user sign-in status
            val logOutMenuItem = binding.navigationView.menu.findItem(R.id.logInOut)

            if (isUserSignedIn) {
                logOutMenuItem.title = "Log Out"
                logOutMenuItem.setIcon(R.drawable.logout)
            } else {
                logOutMenuItem.title = "Sign In"
                logOutMenuItem.setIcon(R.drawable.login)
            }
        }

        // Observe allTasksList LiveData and update the adapter
        sharedViewModel.allTasksList.observe(viewLifecycleOwner) { allTasksList ->
            adapter.updateTaskLists(allTasksList)
            onEmptyLayout(allTasksList?.isEmpty() ?: true)
        }

        // Observe filteredTaskList LiveData and update the adapter for searchQuery
        sharedViewModel.filteredTaskList.observe(viewLifecycleOwner) { filteredTaskList ->
            adapter.updateTaskLists(filteredTaskList)
        }

        // Set up SearchView's query text listener
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                sharedViewModel.filterTasks(newText, "search")
                return true
            }
        })
    }

    private fun registerEvents() {
        binding.fab.setOnClickListener {

            if (sharedViewModel.isUserSignedIn.value == true) {
                bottomSheetFragment = BottomSheetFragment()
                bottomSheetFragment.setListener(this)
                bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
                Toast.makeText(
                    context,
                    "Sign in to continue\nOffline mode coming soon!",
                    Toast.LENGTH_SHORT
                ).show()
            }

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
        sharedViewModel.checkUserAuthState()
        sharedViewModel.resetList() // Clear the RecyclerView
        onEmptyLayout(true) // Show the empty layout
    }

    private fun getStarredList() {
        sharedViewModel.filterTasks("", "star")
    }

    private fun sortCurrentList() {
        //TODO
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
        val dialogBinding = DialogLogoutConfirmationBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnLogoutConfirm.setOnClickListener {
            // Call the ViewModel's logout method to sign out the user
            sharedViewModel.logout()

            // Dismiss the dialog
            dialog.dismiss()

            resetCurrentList()
        }

        dialogBinding.btnLogoutCancel.setOnClickListener {
            // Dismiss the dialog
            dialog.dismiss()
        }

        dialog.show()
    }

}