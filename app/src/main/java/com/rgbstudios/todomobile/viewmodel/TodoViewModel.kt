package com.rgbstudios.todomobile.viewmodel

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.WorkManager
import com.google.gson.Gson
import com.rgbstudios.todomobile.TodoMobileApplication
import com.rgbstudios.todomobile.data.entity.TaskEntity
import com.rgbstudios.todomobile.data.entity.UserEntity
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import com.rgbstudios.todomobile.model.TaskList
import kotlinx.coroutines.launch
import java.util.UUID

class TodoViewModel(private val application: TodoMobileApplication) : ViewModel() {

    private val repository = application.repository
    val firebase = FirebaseAccess()

    // LiveData to hold userEntity
    private val _currentUser = MutableLiveData<UserEntity>()
    val currentUser: LiveData<UserEntity> = _currentUser

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
        startUserListener()
        startTasksListener()
    }

    private fun convertTasksToJson(tasks: List<TaskEntity>): String {
        val gson = Gson()
        return gson.toJson(tasks)
    }

    // Get user from database
    fun startUserListener() {
        viewModelScope.launch {
            repository.getUserFromLocalDatabase().collect { user ->
                // Set currentUser value
                _currentUser.postValue(user)

                // Upload userDetails
                uploadUserDetailsInBackground(user)
            }
        }
    }

    // Get tasks list from Database
    fun startTasksListener() {
        viewModelScope.launch {
            repository.getTasksFromLocalDatabase().collect { taskEntities ->
                val sortedTaskList = sortDatabaseList(taskEntities)
                _allTasksList.postValue(sortedTaskList)

                val newData = convertTasksToJson(taskEntities)

                _currentUser.value?.let { user ->
                    uploadTasksInBackground(user, newData)
                }
            }
        }
    }

    private fun sortDatabaseList(taskEntities: List<TaskEntity>): List<TaskList> {
        val groupedTasks = taskEntities.groupBy { it.taskCompleted }

        val completedList = groupedTasks[true] ?: emptyList()
        val uncompletedList = groupedTasks[false] ?: emptyList()

        val uncompletedTasksList = TaskList(UNCOMPLETED, uncompletedList)
        val completedTasksList = TaskList(COMPLETED, completedList)

        return listOf(uncompletedTasksList, completedTasksList)
    }

    // Function to set up work for uploading tasks
    private fun uploadTasksInBackground(user: UserEntity, newDataJson: String) {
        val data = Data.Builder()
            .putString(USERID, user.userId)
            .putString(NEWDATAJSON, newDataJson)
            .build()

        repository.enqueueUploadTasksWork(data, application.applicationContext)
    }

    // Function to set up work for uploading avatar
    private fun uploadAvatarInBackground(userId: String, avatarFilePath: String) {

        if (avatarFilePath != NULL) {
            val data = Data.Builder()
                .putString(USERID, userId)
                .putString(FILEPATH, avatarFilePath)
                .build()

            repository.enqueueUploadAvatarWork(data, application.applicationContext)
        }
    }

    // Function to set up work for uploading userDetails
    private fun uploadUserDetailsInBackground(user: UserEntity?) {
        if (user != null) {
            val data = Data.Builder()
                .putString(USERID, user.userId)
                .putString(NAME, user.name)
                .putString(OCCUPATION, user.occupation)
                .putString(FILEPATH, user.avatarFilePath ?: NULL)
                .build()

            repository.enqueueUploadUserDetailsWork(data, application.applicationContext)
        }
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


                val newUser =
                    repository.setUpNewUserInDatabase(userId, email, userAvatarData, sender)
                firebase.setUserId(newUser.userId)

                if (sender == SIGNUP) {
                    uploadAvatarInBackground(newUser.userId, newUser.avatarFilePath ?: NULL)
                }

                firebase.addLog("sign in successful")
            }

            callback(true)
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
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

                callback(true)
            } catch (e: Exception) {
                firebase.recordCaughtException(e)
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

                callback(true)
            } catch (e: Exception) {
                firebase.recordCaughtException(e)
                callback(false)
            }
        }
    }

    // Function to delete a task from the database
    fun deleteTask(taskId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteTaskFromDatabase(taskId)

                callback(true)
            } catch (e: Exception) {
                firebase.recordCaughtException(e)
                callback(false)
            }
        }
    }

    // Function to cancel all scheduled work
    fun cancelAllWork() {
        val workManager = WorkManager.getInstance(application.applicationContext)
        workManager.cancelAllWork()
    }

    fun logOut(callback: (Boolean, String?) -> Unit) {

        firebase.logOut { logOutSuccessful, errorMessage ->
            if (logOutSuccessful) {
                firebase.addLog("sign out successful")

                // Save the current user's Id before returning
                currentUser.value?.userId?.let { repository.saveCurrentUserId(it) }
                firebase.setUserId("")

                resetLists()
                callback(true, null)
            } else {
                firebase.addLog("sign out failed")
                callback(false, errorMessage)
            }
        }
    }

    private fun resetLists() {
        _filteredTaskList.value = emptyList()
        _allTasksList.value = emptyList()
    }

    fun filterTasks(query: String?, condition: String) {
        Log.d("aaaaVM", "offender function entered")
        val allTasksList = _allTasksList.value ?: emptyList()

        when (condition) {
            SEARCH -> {
                val filteredTaskEntities = allTasksList
                    .flatMap { it.list }
                    .filter { it.title.contains(query!!, ignoreCase = true) }

                _filteredTaskList.value = groupFilteredTasks(condition, filteredTaskEntities)

            }

            STAR -> {
                val starredTaskEntities = allTasksList
                    .flatMap { it.list }
                    .filter { it.starred }

                _filteredTaskList.value = groupFilteredTasks(condition, starredTaskEntities)
            }
        }
    }

    private fun groupFilteredTasks(filter: String, taskEntities: List<TaskEntity>): List<TaskList> {
        val groupedTasks = taskEntities.groupBy { it.taskCompleted }
        val completedList = groupedTasks[true] ?: emptyList()
        val uncompletedList = groupedTasks[false] ?: emptyList()

        val uncompletedTasksList = TaskList(filter, uncompletedList)
        val completedTasksList = TaskList(COMPLETED, completedList)

        return listOf(uncompletedTasksList, completedTasksList)
    }

    fun changeUserAvatar(
        newAvatarBitmap: Bitmap,
        context: Context,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val currentUser = _currentUser.value

            try {
                if (currentUser != null) {
                    val userId = currentUser.userId

                    // Set avatar to file and get the filepath
                    val newAvatarFilePath =
                        repository.saveAvatarBitmapToFile(userId, context, newAvatarBitmap)

                    // Upload the avatar to firebase storage in the background
                    uploadAvatarInBackground(userId, newAvatarFilePath ?: NULL)

                    // Update the local database with the filepath
                    if (newAvatarFilePath != null) {

                        val userEntity = UserEntity(
                            userId = currentUser.userId,
                            name = currentUser.name,
                            email = currentUser.email,
                            occupation = currentUser.occupation,
                            avatarFilePath = newAvatarFilePath,
                        )
                        repository.updateUserInDatabase(userEntity)

                        callback(true)
                    } else {
                        callback(false)
                    }
                } else {
                    callback(false)
                }

            } catch (e: Exception) {
                firebase.recordCaughtException(e)
                callback(false)
            }
        }
    }

    fun updateUserDetails(name: String, occupation: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val currentUser = _currentUser.value

            try {
                if (currentUser != null) {
                    val userEntity = UserEntity(
                        userId = currentUser.userId,
                        name = name,
                        email = currentUser.email,
                        occupation = occupation,
                        avatarFilePath = currentUser.avatarFilePath,
                    )
                    repository.updateUserInDatabase(userEntity)
                    callback(true)
                } else {
                    callback(false)
                }

            } catch (e: Exception) {
                firebase.recordCaughtException(e)
                callback(false)
            }
        }
    }

    // Function to set the selected task data to be edited in the LiveData
    fun setSelectedTaskData(taskData: TaskEntity) {
        _selectedTaskData.value = taskData
    }

    companion object {
        private const val TAG = "TodoViewModel"
        private const val STAR = "Favorites"
        private const val SEARCH = "Search Results"
        private const val USERID = "userId"
        private const val COMPLETED = "completed"
        private const val UNCOMPLETED = "uncompleted"
        private const val NEWDATAJSON = "newDataJson"
        private const val SIGNUP = "SignUpFragment"
        private const val NULL = "null"
        private const val NAME = "name"
        private const val OCCUPATION = "occupation"
        private const val FILEPATH = "avatarFilePath"

    }
}