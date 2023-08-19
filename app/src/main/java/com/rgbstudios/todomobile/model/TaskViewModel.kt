package com.rgbstudios.todomobile.model

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
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
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class TaskViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currentUser: FirebaseUser? = auth.currentUser
    private val userId: String? = currentUser?.uid
    private val databaseListRef: DatabaseReference =
        FirebaseDatabase.getInstance().reference.child("users").child(userId.orEmpty())
            .child("tasks")
    private val databaseUserDetailsRef: DatabaseReference =
        FirebaseDatabase.getInstance().reference.child("users").child(userId.orEmpty())
            .child("userDetails")

    // LiveData to hold the user's authentication state
    private val _isUserSignedIn = MutableLiveData<Boolean>()
    val isUserSignedIn: LiveData<Boolean> = _isUserSignedIn

    // LiveData to hold the user avatar
    private val _userAvatarUrl = MutableLiveData<String>()
    val userAvatarUrl: LiveData<String> = _userAvatarUrl

    // LiveData to hold the user email
    private val _userEmail = MutableLiveData<String>()
    val userEmail: LiveData<String> = _userEmail

    // LiveData to hold user details from firebase
    private val _userDetailsFromFirebase = MutableLiveData<UserDetails>()
    val userDetailsFromFirebase: LiveData<UserDetails> = _userDetailsFromFirebase

    // LiveData to hold task list from firebase
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
        checkUserAuthState()
        fetchUserAvatarUrl()
        getUserDetailsFromFirebase()
    }

    fun uploadUserAvatar(bitmap: Bitmap, callback: (Boolean) -> Unit) {
        if (userId != null) {
            val storageReference =
                FirebaseStorage.getInstance().reference.child("avatars").child(userId)

            // Convert the Bitmap to a ByteArrayOutputStream
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val avatarData = outputStream.toByteArray()

            // Upload the avatar image to Firebase Storage
            val uploadTask = storageReference.putBytes(avatarData)
            uploadTask.addOnSuccessListener {
                callback(true)
                // Image upload successful
                fetchUserAvatarUrl()
            }.addOnFailureListener {
                callback(false)
                Log.d("Avatar", it.message.toString())
            }

        }

    }

    private fun fetchUserAvatarUrl() {
        if (userId != null) {
            val storageReference = FirebaseStorage.getInstance().reference
                .child("avatars")
                .child(userId)

            storageReference.downloadUrl.addOnSuccessListener { uri ->

                // Set the avatar URL in the LiveData
                _userAvatarUrl.value = uri.toString()
            }.addOnFailureListener {

                // Handle failure to fetch avatar URL if needed
                Log.d("Avatar", it.message.toString())
            }
        }
    }

    fun saveUserEmailReferenceOnAuth(email: String) {
        _userEmail.value = email
    }

    // Function to set the selected task data to be edited in the LiveData
    fun setSelectedTaskData(taskData: TaskDataFromFirebase) {
        _selectedTaskData.value = taskData
    }

    // Function to save a new task to the database
    fun saveTask(
        title: String,
        description: String,
        starred: Boolean,
        callback: (Boolean) -> Unit
    ) {

        if (userId == null) {
            // The user is not authenticated, cannot perform the operation.
            callback(false)
            return
        }

        val taskData = TaskData(title, description, false, starred)
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
        starred: Boolean,
        callback: (Boolean) -> Unit
    ) {
        if (userId == null) {
            // The user is not authenticated, cannot perform the operation.
            callback(false)
            return
        }

        val databaseTaskRef = databaseListRef.child(id)
        val newTaskData = TaskData(title, description, completed, starred)
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
                        val starred =
                            item.child("starred").getValue(Boolean::class.java) ?: false
                        TaskDataFromFirebase(it, title, description, taskCompleted, starred)
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
                task.taskCompleted -> {
                    completedList.add(task)
                }

                !task.taskCompleted -> {
                    uncompletedList.add(task)
                }

                task.starred -> {
                    starredList.add(task)
                }
            }
        }
        val uncompletedTasksList = TaskList("uncompleted", uncompletedList)
        val completedTasksList = TaskList("completed", completedList)
        val starredTasksList = TaskList("starred", starredList)

        _allTasksList.value = listOf(uncompletedTasksList, completedTasksList, starredTasksList)

    }

    fun filterTasks(query: String?, condition: String) {
        val allTasksList = _allTasksList.value ?: emptyList()
        var filteredList = ArrayList<TaskList>()

        when (condition) {
            "search" -> {
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

            "star" -> {
                val filteredTaskList =
                    allTasksList.filter { it.name == "starred" && it.list.isNotEmpty() }
                _filteredTaskList.value = filteredTaskList
            }
        }
    }

    fun resetList() {
        _listFromFirebase.value = emptyList()
        _allTasksList.value = emptyList()
        _filteredTaskList.value = emptyList()
        _userDetailsFromFirebase.value = UserDetails("", "")
    }

    fun checkUserAuthState() {
        _isUserSignedIn.value = false

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

    fun logout() {
        auth.signOut()
    }

    fun updateUserDetails(
        name: String,
        occupation: String,
        callback: (Boolean) -> Unit
    ) {
        if (userId == null) {
            // The user is not authenticated, cannot perform the operation.
            callback(false)
            return
        }

        val userDetails = UserDetails(name, occupation)
        databaseUserDetailsRef.setValue(userDetails)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Return success result
                    callback(true)
                } else {
                    // Return failure result
                    callback(false)
                }
            }
    }


    private fun getUserDetailsFromFirebase() {
        if (userId == null) {
            // The user is not authenticated, cannot perform the operation.
            return
        }

        databaseUserDetailsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val name = snapshot.child("name").getValue(String::class.java) ?: ""
                val occupation =
                    snapshot.child("occupation").getValue(String::class.java) ?: ""
                val userDataFromFirebase = UserDetails(name, occupation)
                _userDetailsFromFirebase.value = userDataFromFirebase
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("getUserDetails", error.message)
            }
        })

        if (_isUserSignedIn.value == true) {
            _userEmail.value = currentUser?.email
        }
    }


}