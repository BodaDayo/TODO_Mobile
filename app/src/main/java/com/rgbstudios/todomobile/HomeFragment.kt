package com.rgbstudios.todomobile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.textfield.TextInputEditText
import com.rgbstudios.todomobile.adapter.ListAdapter
import com.rgbstudios.todomobile.databinding.FragmentHomeBinding
import com.rgbstudios.todomobile.model.TaskViewModel


class HomeFragment : Fragment(), BottomSheetFragment.DialogAddTaskBtnClickListener {

    private val sharedViewModel: TaskViewModel by activityViewModels()

    private lateinit var binding: FragmentHomeBinding
    private lateinit var bottomSheetFragment: BottomSheetFragment
    private lateinit var adapter: ListAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

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

        binding.toggleNav.setOnClickListener {
            // Get a reference to the activity and its DrawerLayout
            val activity = requireActivity()
            val drawerLayout = activity.findViewById<DrawerLayout>(R.id.drawerLayout)

            // Toggle the navigation drawer open or close
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Observe the isUserSignedIn LiveData from the sharedViewModel
        sharedViewModel.isUserSignedIn.observe(viewLifecycleOwner) { isUserSignedIn ->
            if (!isUserSignedIn) {
                // User is signed out, reset the list or take any necessary actions
                resetCurrentList()
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
            bottomSheetFragment = BottomSheetFragment()
            bottomSheetFragment.setListener(this)
            bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
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

    private fun onEmptyLayout(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptinessLayout.visibility = View.VISIBLE
            binding.searchViewLayout.visibility = View.GONE
        } else {
            binding.emptinessLayout.visibility = View.GONE
            binding.searchViewLayout.visibility = View.VISIBLE
        }
    }

    // This function will be called when you want to stop the refreshing animation
    private fun stopRefreshing() {
        swipeRefreshLayout.isRefreshing = false
    }

    private fun resetCurrentList() {
        sharedViewModel.resetList() // Clear the RecyclerView
        onEmptyLayout(true) // Show the empty layout
    }

    private fun getStarredList(){
        //TODO
    }

    private fun sortCurrentList(){
        //TODO
    }

    private fun toggleNavigationDrawer(){
        // Get a reference to the activity and its DrawerLayout
        val activity = requireActivity()
        val drawerLayout = activity.findViewById<DrawerLayout>(R.id.drawerLayout)

        // Toggle the navigation drawer open or close
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }
}