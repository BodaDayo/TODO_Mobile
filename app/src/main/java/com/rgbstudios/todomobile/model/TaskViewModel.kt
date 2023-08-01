package com.rgbstudios.todomobile.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TaskViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currentUser: FirebaseUser? = auth.currentUser
    private val userId: String? = currentUser?.uid
    private val databaseListRef: DatabaseReference =
        FirebaseDatabase.getInstance().reference.child("users").child(userId.orEmpty())


    // LiveData to hold the user's authentication state
    private val _isUserSignedIn = MutableLiveData<Boolean>()
    val isUserSignedIn: LiveData<Boolean> = _isUserSignedIn

    // LiveData to hold list from firebase
    private val _listFromFirebase = MutableLiveData<List<TaskDataFromFirebase>>()
    val listFromFirebase: LiveData<List<TaskDataFromFirebase>> = _listFromFirebase

    // LiveData to hold allTasksList
    private val _allTasksList = MutableLiveData<List<TaskList>>()
    val allTasksList: LiveData<List<TaskList>> = _allTasksList

    // LiveData to hold the filtered task list
    private val _filteredTaskList = MutableLiveData<List<TaskList>>()
    val filteredTaskList: LiveData<List<TaskList>> = _filteredTaskList


    // LiveData to hold the selected task data
    private val _selectedTaskData = MutableLiveData<TaskDataFromFirebase>()
    val selectedTaskData: LiveData<TaskDataFromFirebase> = _selectedTaskData


    init {
        setupAuthStateListener()
    }


    // Function to set the selected task data to be edited in the LiveData
    fun setSelectedTaskData(taskData: TaskDataFromFirebase) {
        _selectedTaskData.value = taskData
    }

    // Function to save a new task to the database
    fun saveTask(title: String, description: String, callback: (Boolean) -> Unit) {

        if (userId == null) {
            // The user is not authenticated, cannot perform the operation.
            callback(false)
            return
        }

        val taskData = TaskData(title, description, false)
        databaseListRef.push().setValue(taskData).addOnCompleteListener(OnCompleteListener { task ->
            if (task.isSuccessful) {
                // Return success result
                callback(true)
            } else {
                // Return failure result
                callback(false)
            }
        })
    }

    // Function to update a task in the database
    fun updateTask(
        id: String,
        title: String,
        description: String,
        completed: Boolean,
        callback: (Boolean) -> Unit
    ) {
        if (userId == null) {
            // The user is not authenticated, cannot perform the operation.
            callback(false)
            return
        }

        val databaseTaskRef = databaseListRef.child(id)
        val newTaskData = TaskData(title, description, completed)
        databaseTaskRef.setValue(newTaskData).addOnCompleteListener(OnCompleteListener { task ->
            if (task.isSuccessful) {
                // Return success result
                callback(true)
            } else {
                // Return failure result
                callback(false)
            }
        })
    }

    // Function to delete a task from the database
    fun deleteTask(id: String, callback: (Boolean) -> Unit) {
        if (userId == null) {
            // The user is not authenticated, cannot perform the operation.
            callback(false)
            return
        }

        val databaseTaskRef = databaseListRef.child(id)
        databaseTaskRef.removeValue().addOnCompleteListener(OnCompleteListener { task ->
            if (task.isSuccessful) {
                // Return success result
                callback(true)
            } else {
                // Return failure result
                callback(false)
            }
        })
    }

    // Get tasks list from Firebase
    fun getTasksFromFirebase() {
        if (userId == null) {
            // The user is not authenticated, cannot perform the operation.
            return
        }

        databaseListRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _listFromFirebase.value = emptyList()
                val collectorList = mutableListOf<TaskDataFromFirebase>()
                for (item in snapshot.children) {
                    val taskDataFromFirebase = item.key?.let {
                        val title = item.child("title").getValue(String::class.java) ?: ""
                        val description =
                            item.child("description").getValue(String::class.java) ?: ""
                        val taskCompleted =
                            item.child("taskCompleted").getValue(Boolean::class.java) ?: false
                        TaskDataFromFirebase(it, title, description, taskCompleted)
                    }

                    if (taskDataFromFirebase != null) {
                        collectorList.add(taskDataFromFirebase)
                    }
                }
                _listFromFirebase.value = collectorList

                sortFirebaseList()
            }

            override fun onCancelled(error: DatabaseError) {
                // TODO Look for Solution
            }

        })
    }

    // sort firebase list based on completed and starred status
    private fun sortFirebaseList() {

        _allTasksList.value = emptyList()

        val listToSort = _listFromFirebase.value ?: emptyList()
        val uncompletedList = mutableListOf<TaskDataFromFirebase>()
        val completedList = mutableListOf<TaskDataFromFirebase>()
        val starredList = mutableListOf<TaskDataFromFirebase>()

        for (task in listToSort) {
            when {
                task.taskCompleted == true -> {
                    completedList.add(task)
                }

                task.taskCompleted == false -> {
                    uncompletedList.add(task)
                }

                task.taskId == "" -> {
                    starredList.add(task)
                }
            }
        }
        val uncompletedTasksList = TaskList("uncompleted", uncompletedList)
        val completedTasksList = TaskList("completed", completedList)
        val starredTasksList = TaskList("starred", starredList)

        _allTasksList.value = listOf(uncompletedTasksList, completedTasksList, starredTasksList)

    }

    fun filterTasks(query: String?) {
        val allTasksList = _allTasksList.value ?: emptyList()
        val filteredList = ArrayList<TaskList>()

        if (query != null) {
            for (taskList in allTasksList) {
                val filteredTasks = taskList.list.filter { task ->
                    task.title.contains(query, ignoreCase = true)
                }
                if (filteredTasks.isNotEmpty()) {
                    val filteredTaskList = TaskList(taskList.name, filteredTasks)
                    filteredList.add(filteredTaskList)
                }
            }
        }
        _filteredTaskList.value = filteredList
    }


    fun resetList() {
        _listFromFirebase.value = emptyList()
        _allTasksList.value = emptyList()
    }
    private fun setupAuthStateListener() {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in
                _isUserSignedIn.value = true
            } else {
                // User is signed out
                _isUserSignedIn.value = false
                // Perform any necessary actions when the user signs out
                // For example, you might want to reset the lists or clear any data related to the user.
                resetList()
            }
        }

        // Add the AuthStateListener to the FirebaseAuth instance
        auth.addAuthStateListener(authStateListener)

        // Be sure to remove the AuthStateListener when it's no longer needed (e.g., when the ViewModel is cleared)
        // This avoids potential memory leaks.
        auth.removeAuthStateListener(authStateListener)
    }


}