package com.rgbstudios.todomobile.viewmodel

import android.content.ContentValues.TAG
import android.content.Context
import android.content.res.Resources
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.rgbstudios.todomobile.TodoMobileApplication
import com.rgbstudios.todomobile.data.entity.TaskEntity
import com.rgbstudios.todomobile.data.entity.UserEntity
import com.rgbstudios.todomobile.model.TaskList
import kotlinx.coroutines.launch
import java.util.UUID

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import com.google.gson.Gson
import com.rgbstudios.todomobile.worker.UploadAvatarWorker
import com.rgbstudios.todomobile.worker.UploadTasksWorker

import java.util.concurrent.TimeUnit


class TodoViewModel(private val application: TodoMobileApplication) : ViewModel() {

    private val repository = application.repository

    // LiveData to hold userEntity
    private val _currentUser = MutableLiveData<UserEntity>()
    private val currentUser: LiveData<UserEntity> = _currentUser

    // LiveData to hold the avatar file path
    private val _avatarFilePath = MutableLiveData<String>()
    val avatarFilePath: LiveData<String> = _avatarFilePath

    private val dataChangeListenerStarted = MutableLiveData(false)

    // LiveData to hold allTasksList
    private val _allTasksList = MutableLiveData<List<TaskList>>()
    val allTasksList: LiveData<List<TaskList>> = _allTasksList

    private val tasksToExport = MutableLiveData<List<TaskEntity>>()

    init {
        startUserListener()
        startTasksListener()
    }

    // Function to update the avatar file path
    fun updateAvatarFilePath(newPath: String) {


        _avatarFilePath.value = newPath
    }

    private fun startDataChangeListener() {
        viewModelScope.launch {
            dataChangeListenerStarted.value = true
            Log.d("aaaaDCL", "started")

            repository.observeHasPendingChanges().collect { hasPendingChanges ->
                Log.d("aaaaDCL", "detected")

                if (hasPendingChanges) {
                    Log.d("aaaaDCL", "yes, has pending changes")

                    val tasksData = tasksToExport.value
                    Log.d("aaaaDCL", "fresh data:$tasksData")

                    if (tasksData != null) {
                        val newData = convertTasksToJson(tasksData)

                        Log.d("aaaaDCL", "converted data:$newData")
                        _currentUser.value?.let { user ->
                            setUpOneTimeTaskUpload(user, newData)
                            repository.setPendingChangesFlag(false)
                        }
                    }
                }
            }
        }
    }

    private fun convertTasksToJson(tasks: List<TaskEntity>): String {
        val gson = Gson()
        return gson.toJson(tasks)
    }

    private fun convertUserToJson(user: UserEntity): String {
        val gson = Gson()
        return gson.toJson(user)
    }

    // Get user from database
    private fun startUserListener() {
        viewModelScope.launch {
            repository.getUserFromLocalDatabase().collect { user ->
                _currentUser.postValue(user)

                if (_currentUser.value != null && dataChangeListenerStarted.value == false) {
                    startDataChangeListener()
                }
            }
        }
    }

    // Get tasks list from Database
    fun startTasksListener() {
        viewModelScope.launch {
            repository.getTasksFromLocalDatabase().collect { taskEntities ->
                tasksToExport.postValue(taskEntities)
                val sortedTaskList = sortDatabaseList(taskEntities)
                _allTasksList.postValue(sortedTaskList)
            }
        }
    }

    private fun sortDatabaseList(taskEntities: List<TaskEntity>): List<TaskList> {
        val groupedTasks = taskEntities.groupBy { it.taskCompleted }

        val completedList = groupedTasks[true] ?: emptyList()
        val uncompletedList = groupedTasks[false] ?: emptyList()

        val uncompletedTasksList = TaskList("uncompleted", uncompletedList)
        val completedTasksList = TaskList("completed", completedList)

        return listOf(uncompletedTasksList, completedTasksList)
    }

    // Function to set up work for uploading tasks
    private fun setUpOneTimeTaskUpload(user: UserEntity, newDataJson: String) {

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Requires network connection
            .build()

        val data = Data.Builder()
            .putString("userId", user.userId)
            .putString("newDataJson", newDataJson)
            .build()

        val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadTasksWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .build()

        WorkManager.getInstance(application.applicationContext)
            .enqueueUniqueWork(
                "uploadUserTasks",
                ExistingWorkPolicy.REPLACE,
                uploadWorkRequest
            )
    }

    // Function to set up work for uploading avatar
    private fun setUpOneTimeAvatarUpload(user: String) {

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Requires network connection
            .build()

        val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadAvatarWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf("user" to user))
            .build()

        WorkManager.getInstance(application.applicationContext)
            .enqueueUniqueWork(
                "uploadUserAvatar",
                ExistingWorkPolicy.REPLACE,
                uploadWorkRequest
            )
    }

    private fun setUpPeriodicTaskUpload(userId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Requires network connection
            .build()

        val uploadWorkRequest = PeriodicWorkRequestBuilder<UploadTasksWorker>(
            repeatInterval = 2,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
            //repeatInterval = 1, // Repeat every 1 day
            //repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInputData(workDataOf("userId" to userId)) // Pass userId to Worker
            .build()

        WorkManager.getInstance(application.applicationContext).enqueueUniquePeriodicWork(
            "uploadUserTasks",
            ExistingPeriodicWorkPolicy.UPDATE,
            uploadWorkRequest
        )
    }

    fun setUpNewUser(
        userId: String,
        email: String,
        context: Context,
        resources: Resources,
        sender: String,
        callback: (Boolean) -> Unit
    ) {
        try {
            viewModelScope.launch {
                // Get the avatar data
                val userAvatarData = repository.getAvatar(userId, sender, context, resources)


                val newUser = repository.setUpNewUserInDatabase(userId, email, userAvatarData, sender)

                if (sender == "SignUpFragment") {
                    val userData = convertUserToJson(newUser)
                    setUpOneTimeAvatarUpload(userData)
                }

                newUser.avatarFilePath?.let {
                    updateAvatarFilePath(it)
                    Log.d("aaaa", "$avatarFilePath")
                }
            }

            callback(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up new user: ${e.message}", e)
            callback(false)
        }
    }


    // Function to save a new task to the database
    fun saveTask(
        title: String,
        description: String,
        starred: Boolean,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val taskId = UUID.randomUUID().toString()

            val taskEntity = TaskEntity(
                taskId = taskId,
                title = title,
                description = description,
                taskCompleted = false,
                starred = starred
            )
            try {
                repository.saveTaskToDatabase(taskEntity)

                // Set changes flag for workManager to perform database sync
                repository.setPendingChangesFlag(true)

                callback(true)

            } catch (e: Exception) {
                Log.e(TAG, "Error saving task in ViewModel: ${e.message}", e)
                callback(false)
            }
        }
    }

    // Function to update a task in the database
    fun updateTask(
        taskId: String,
        title: String,
        description: String,
        completed: Boolean,
        starred: Boolean,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val taskEntity = TaskEntity(
                taskId = taskId,
                title = title,
                description = description,
                taskCompleted = completed,
                starred = starred
            )
            try {
                repository.updateTaskInDatabase(taskEntity)

                // Set changes flag for workManager to perform database sync
                repository.setPendingChangesFlag(true)
                callback(true)
            } catch (e: Exception) {

                Log.e(TAG, "Error deleting task in ViewModel: ${e.message}", e)
                callback(false)
            }
        }
    }

    // Function to delete a task from the database
    fun deleteTask(taskId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteTaskFromDatabase(taskId)

                // Set changes flag for workManager to perform database sync
                repository.setPendingChangesFlag(true)
                callback(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting task in ViewModel: ${e.message}", e)
                callback(false)
            }
        }
    }

    // Function to cancel all scheduled work
    fun cancelAllWork() {
        val workManager = WorkManager.getInstance(application.applicationContext)
        workManager.cancelAllWork()
    }


    /*

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
    private val _listFromFirebase = MutableLiveData<List<TaskEntity>>()
    val listFromFirebase: LiveData<List<TaskEntity>> = _listFromFirebase

    // LiveData to hold allTasksList
    private val _allTasksList = MutableLiveData<List<TaskList>>()
    val allTasksList: LiveData<List<TaskList>> = _allTasksList

    // LiveData to hold the filtered task list
    private val _filteredTaskList = MutableLiveData<List<TaskList>>()
    val filteredTaskList: LiveData<List<TaskList>> = _filteredTaskList


    // LiveData to hold the selected task data
    private val _selectedTaskData = MutableLiveData<TaskEntity>()
    val selectedTaskData: LiveData<TaskEntity> = _selectedTaskData


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
    fun setSelectedTaskData(taskData: TaskEntity) {
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
/*
        val taskItem = TaskEntity(
            userId = userId,
            title = title,
            description = description,
            taskCompleted = false,
            starred = starred
        )
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.insertTask(taskItem)
            callback(true)
        }*/
    }

    // Function to update a task in the database
    fun updateTask(
        taskId: String,
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
        /*
        val taskItem = TaskEntity(taskId, userId, title, description, completed, starred)
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.updateTask(taskItem)
            callback(true)
        }*/
    }

    // Function to delete a task from the database
    fun deleteTask(taskId: Int, callback: (Boolean) -> Unit) {
        if (userId == null) {
            // The user is not authenticated, cannot perform the operation.
            callback(false)
            return
        }
/*
        viewModelScope.launch(Dispatchers.IO) {
            val deletedCount = taskDao.deleteTask(userId, taskId)
            callback(deletedCount > 0)
        }
*/

        /*
        val databaseTaskRef = databaseListRef.child(id)
        databaseTaskRef.removeValue().addOnCompleteListener(OnCompleteListener { task ->
            if (task.isSuccessful) {
                // Return success result
                callback(true)
            } else {
                // Return failure result
                callback(false)
            }
        }) */
    }

    // Get tasks list from Firebase
    fun getTasksFromDatabase() {
        if (userId == null) {
            // The user is not authenticated, cannot perform the operation.
            return
        }
        /*
        databaseListRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _listFromFirebase.value = emptyList()
                val collectorList = mutableListOf<TaskEntity>()
                for (item in snapshot.children) {
                    val taskDataFromFirebase = item.key?.let {
                        val title = item.child("title").getValue(String::class.java) ?: ""
                        val description =
                            item.child("description").getValue(String::class.java) ?: ""
                        val taskCompleted =
                            item.child("taskCompleted").getValue(Boolean::class.java) ?: false
                        val starred =
                            item.child("starred").getValue(Boolean::class.java) ?: false
                        TaskEntity(it, userId, title, description, taskCompleted, starred)
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

        })*/
    }

    // sort firebase list based on completed and starred status
    private fun sortFirebaseList() {

        _allTasksList.value = emptyList()

        /*val listToSort = _listFromFirebase.value ?: emptyList()
        val uncompletedList = mutableListOf<TaskEntity>()
        val completedList = mutableListOf<TaskEntity>()
        val starredList = mutableListOf<TaskEntity>()


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

        _allTasksList.value = listOf(uncompletedTasksList, completedTasksList, starredTasksList)*/

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
       // _listFromFirebase.value = emptyList()
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

                // Update the user details
                _userDetailsFromFirebase.value = userDataFromFirebase

                // Update the user email
                _userEmail.value = currentUser?.email
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("getUserDetails", error.message)
            }
        })

        if (_isUserSignedIn.value == true) {
            _userEmail.value = currentUser?.email
        }
    }

     */
}