package com.rgbstudios.todomobile.viewmodel

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.EmailAuthProvider
import com.google.gson.Gson
import com.rgbstudios.todomobile.TodoMobileApplication
import com.rgbstudios.todomobile.data.entity.CategoryEntity
import com.rgbstudios.todomobile.data.entity.TaskEntity
import com.rgbstudios.todomobile.data.entity.UserEntity
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import com.rgbstudios.todomobile.model.TaskList
import com.rgbstudios.todomobile.utils.SharedPreferencesManager
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class TodoViewModel(private val application: TodoMobileApplication) : ViewModel() {

    private val repository = application.repository
    private val firebase = FirebaseAccess()
    private val sharedPreferences = SharedPreferencesManager(application)

    // LiveData to hold the isFirstLaunch status
    private val _isFirstLaunch = MutableLiveData<Boolean>()
    val isFirstLaunch: LiveData<Boolean> = _isFirstLaunch

    // LiveData to hold the isBiometricEnabled status
    private val _isBiometricEnabled = MutableLiveData<Boolean>()
    val isBiometricEnabled: LiveData<Boolean> = _isBiometricEnabled

    // LiveData to hold the isBiometricEnabled status
    private val _isGoogleConnected = MutableLiveData<Boolean>()
    val isGoogleConnected: LiveData<Boolean> = _isGoogleConnected

    // LiveData to hold the isBiometricEnabled status
    private val _isFacebookConnected = MutableLiveData<Boolean>()
    val isFacebookConnected: LiveData<Boolean> = _isFacebookConnected

    // LiveData to hold the isBiometricEnabled status
    private val _isTwitterConnected = MutableLiveData<Boolean>()
    val isTwitterConnected: LiveData<Boolean> = _isTwitterConnected

    // LiveData to hold userEntity
    private val _currentUser = MutableLiveData<UserEntity>()
    val currentUser: LiveData<UserEntity> = _currentUser

    // All tasks
    private var allTaskEntities: List<TaskEntity> = listOf()

    // LiveData to hold allTasksList
    private val _allTasksList = MutableLiveData<List<TaskList>>()
    val allTasksList: LiveData<List<TaskList>> = _allTasksList

    // LiveData to hold the filtered task list
    private val _filteredTaskList = MutableLiveData<List<TaskList>>()
    val filteredTaskList: LiveData<List<TaskList>> = _filteredTaskList

    // LiveData to hold the selected task data
    private val _selectedTaskData = MutableLiveData<TaskEntity>()
    val selectedTaskData: LiveData<TaskEntity> = _selectedTaskData

    // LiveData to hold the selectionMode status
    private val _isSelectionModeOn = MutableLiveData<Boolean>()
    val isSelectionModeOn: LiveData<Boolean> = _isSelectionModeOn

    // LiveData to hold highlighted tasks list
    private val _highlightedTaskList = MutableLiveData<List<TaskEntity>>()
    val highlightedTaskList: LiveData<List<TaskEntity>> = _highlightedTaskList

    private val _highlightedListName = MutableLiveData<String>()
    val highlightedListName: LiveData<String> = _highlightedListName

    // LiveData to hold the list of categories in the database
    private val _selectedTaskCategories = MutableLiveData<List<CategoryEntity>>()
    val selectedTaskCategories: LiveData<List<CategoryEntity>> = _selectedTaskCategories

    // LiveData to hold the list of categories in the database
    private val _categories = MutableLiveData<List<CategoryEntity>>()
    val categories: LiveData<List<CategoryEntity>> = _categories

    // LiveData to hold the condition to sort the tasks
    private val _settingsItemSelected = MutableLiveData<String>()
    val settingsItemSelected: LiveData<String> = _settingsItemSelected

    // LiveData to hold the isBiometricEnabled status
    private val _closeSlider = MutableLiveData<Boolean>()
    val closeSlider: LiveData<Boolean> = _closeSlider

    // LiveData to hold the isEmailAuthSet status
    private val _isEmailAuthSet = MutableLiveData<Boolean>()
    val isEmailAuthSet: LiveData<Boolean> = _isEmailAuthSet

    init {
        _isFirstLaunch.value = getIsFirstLaunch()
        _isBiometricEnabled.value = getBiometricAuthStatus()
        _isEmailAuthSet.value = getEmailAuthStatus()
        getConnectedAccounts()
        setIsSelectionModeOn(false)
        updateHighlightedListName("")
        setSettingsItem("")
        toggleSlider(false)
        startDatabaseListeners()
    }

    /**
     *-----------------------------------------------------------------------------------------------
     */

    fun startDatabaseListeners() {
        startUserListener()
        startTasksListener()
        startCategoriesListener()
    }

    // Get user from database
    private fun startUserListener() {
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
    private fun startTasksListener() {
        viewModelScope.launch {
            repository.getTasksFromLocalDatabase().collect { taskEntities ->
                allTaskEntities = taskEntities
                val sortedDatabaseList = sortDatabaseList(taskEntities)

                // Default tasks sorting by date
                sortAllTasksList(sortedDatabaseList)

                val newData = convertTasksToJson(taskEntities)

                _currentUser.value?.let { user ->
                    uploadTasksInBackground(user, newData)
                }
            }
        }
    }

    private fun startCategoriesListener() {
        viewModelScope.launch {
            repository.getCategoriesFromDatabase().collect { categoryList ->
                _categories.postValue(categoryList)

                val newData = convertCategoriesToJson(categoryList)

                _currentUser.value?.let { user ->
                    uploadCategoriesInBackground(user.userId, newData)
                }
            }
        }
    }

    /**
     *-----------------------------------------------------------------------------------------------
     */

    fun setUpNewUser(
        userId: String,
        email: String,
        pass: String,
        context: Context,
        resources: Resources,
        sender: String,
        extractedDetails: Pair<String, Uri?>?,
        callback: (Boolean) -> Unit
    ) {
        try {
            viewModelScope.launch {
                // Get the avatar data
                val userAvatarData = repository.getAvatar(
                    userId, sender, context, resources,
                    extractedDetails?.second
                )

                val result =
                    repository.setUpNewUserInDatabase(
                        userId,
                        email,
                        userAvatarData,
                        sender,
                        extractedDetails?.first
                    )
                val newUser = result.first
                val categories = result.second
                firebase.setUserId(newUser.userId)

                if (sender == SIGNUP) {
                    uploadAvatarInBackground(newUser.userId, newUser.avatarFilePath ?: NULL)

                    val newData = convertCategoriesToJson(categories)
                    uploadCategoriesInBackground(newUser.userId, newData)
                }

                firebase.addLog("sign in successful")
            }

            updateAuthenticationData(email, pass)
            fetchProviders()
            updateAuthState()
            callback(true)
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            callback(false)
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

    /**
     *-----------------------------------------------------------------------------------------------
     */

    private fun sortDatabaseList(taskEntities: List<TaskEntity>): List<TaskList> {
        val groupedTasks = taskEntities.groupBy { it.taskCompleted }

        val completedList = groupedTasks[true] ?: emptyList()
        val uncompletedList = groupedTasks[false] ?: emptyList()

        val uncompletedTasksList = TaskList(UNCOMPLETED, uncompletedList)
        val completedTasksList = TaskList(COMPLETED, completedList)

        return listOf(uncompletedTasksList, completedTasksList)
    }

    fun sortAllTasksList(taskLists: List<TaskList>) {
        val sortingCondition = getSortingCondition()

        val uncompletedTasksList = taskLists.first().list
        val completedTasksList = taskLists.last().list

        val sortedTaskList =
            sortTaskListsByCondition(uncompletedTasksList, completedTasksList, sortingCondition)

        _allTasksList.postValue(sortedTaskList)
    }

    private fun sortTaskListsByCondition(
        uncompletedList: List<TaskEntity>,
        completedList: List<TaskEntity>,
        sortingCondition: Pair<String, Boolean>
    ): List<TaskList> {
        val (sortBy, order) = sortingCondition

        val uncompletedSorted = sortTaskEntities(uncompletedList, sortBy, order)
        val completedSorted = sortTaskEntities(completedList, sortBy, order)

        return listOf(
            TaskList(UNCOMPLETED, uncompletedSorted),
            TaskList(COMPLETED, completedSorted)
        )
    }

    private fun sortTaskEntities(
        taskEntities: List<TaskEntity>,
        sortBy: String,
        order: Boolean
    ): List<TaskEntity> {
        return when (sortBy) {

            DATE -> {
                taskEntities.partition { it.dueDateTime != null }
                    .let { (datedList, unDatedList) ->
                        if (order) {
                            datedList.sortedWith(compareBy { it.dueDateTime }) + unDatedList.sortedBy { it.title.lowercase() }
                        } else {
                            val output =
                                unDatedList.sortedByDescending { it.title.lowercase() } + datedList.sortedWith(
                                    compareBy { it.dueDateTime })
                            output.reversed()
                        }
                    }
            }

            TITLE -> {
                if (order) {
                    // Sort by title chronologically
                    taskEntities.sortedBy { it.title.lowercase() }
                } else {
                    // Sort by title in reverse chronological order
                    taskEntities.sortedByDescending { it.title.lowercase() }
                }
            }

            else -> {
                // Default sorting (no change)
                taskEntities
            }
        }
    }

    /**
     *-----------------------------------------------------------------------------------------------
     */

    fun saveTask(
        title: String,
        description: String,
        starred: Boolean,
        dueDateTime: Calendar?,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val taskId = UUID.randomUUID().toString()

            val taskEntity = TaskEntity(
                taskId = taskId,
                title = title,
                description = description,
                taskCompleted = false,
                starred = starred,
                dueDateTime = dueDateTime
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

    fun saveMultipleTask(
        taskEntityList: List<TaskEntity>,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.batchSaveTaskToDatabase(taskEntityList)

                callback(true)
            } catch (e: Exception) {
                firebase.recordCaughtException(e)
                callback(false)
            }
        }
    }

    fun updateTask(
        taskId: String,
        title: String,
        description: String,
        completed: Boolean,
        starred: Boolean,
        dueDateTime: Calendar?,
        categoryIds: List<String>,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {

            val taskEntity = TaskEntity(
                taskId = taskId,
                title = title,
                description = description,
                taskCompleted = completed,
                starred = starred,
                dueDateTime = dueDateTime,
                categoryIds = categoryIds
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

    fun batchDeleteTask(taskIdList: List<String>, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteMultipleTasksFromDatabase(taskIdList)

                callback(true)
            } catch (e: Exception) {
                firebase.recordCaughtException(e)
                callback(false)
            }
        }
    }

    // Function to set the selected task data to be edited in the LiveData
    fun setSelectedTaskData(taskData: TaskEntity) {
        // Set the selected task
        _selectedTaskData.value = taskData

        // Get the list of categories the task belongs to
        val taskCategories = allTaskEntities
            .filter { it.taskId == taskData.taskId }
            .flatMap { it.categoryIds }

        // Get the list of categories the user has from the database
        val categoriesInDatabase = _categories.value

        // Filter the categories that match the taskCategories
        val selectedCategories = categoriesInDatabase?.filter { category ->
            category.categoryId in taskCategories
        }

        // Set _selectedTaskCategories.value with the filtered list
        _selectedTaskCategories.value = selectedCategories ?: emptyList()

    }

    /**
     *-----------------------------------------------------------------------------------------------
     */

    fun saveCategory(
        categoryName: String,
        categoryIconIdentifier: String,
        categoryColorIdentifier: String,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val categoryId = UUID.randomUUID().toString()

            val category = CategoryEntity(
                categoryId = categoryId,
                categoryName = categoryName,
                categoryIconIdentifier = categoryIconIdentifier,
                categoryColorIdentifier = categoryColorIdentifier
            )
            try {
                repository.saveCategoryToDatabase(category)

                callback(true)
            } catch (e: Exception) {
                firebase.recordCaughtException(e)
                callback(false)
            }
        }
    }

    fun updateCategory(
        categoryName: String,
        categoryIconIdentifier: String,
        categoryColorIdentifier: String,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val categoryId = UUID.randomUUID().toString()

            val category = CategoryEntity(
                categoryId = categoryId,
                categoryName = categoryName,
                categoryIconIdentifier = categoryIconIdentifier,
                categoryColorIdentifier = categoryColorIdentifier
            )
            try {
                repository.updateCategoryInDatabase(category)

                callback(true)
            } catch (e: Exception) {
                firebase.recordCaughtException(e)
                callback(false)
            }
        }
    }

    fun deleteCategory(categoryId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteCategoryFromDatabase(categoryId)

                callback(true)
            } catch (e: Exception) {
                firebase.recordCaughtException(e)
                callback(false)
            }
        }
    }

    fun addTaskToCategory(category: CategoryEntity, callback: (Boolean) -> Unit) {
        try {
            _selectedTaskCategories.value =
                (_selectedTaskCategories.value ?: emptyList()) + category

            callback(true)
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            callback(false)
        }
    }

    fun removeTaskFromCategory(category: CategoryEntity, callback: (Boolean) -> Unit) {
        try {
            val selectedTaskCategories = _selectedTaskCategories.value!!

            val updatedCategories = selectedTaskCategories.filterNot { it == category }

            _selectedTaskCategories.value = updatedCategories

            callback(true)

        } catch (e: Exception) {
            firebase.recordCaughtException(e)
            callback(false)
        }

    }

    /**
     *-----------------------------------------------------------------------------------------------
     */

    fun filterTasks(query: String?, condition: String, callback: (Boolean) -> Unit) {
        val allTasksList = _allTasksList.value ?: emptyList()

        when (condition) {
            SEARCH -> {
                val filteredTaskEntities = allTasksList
                    .flatMap { it.list }
                    .filter { it.title.contains(query!!, ignoreCase = true) }

                _filteredTaskList.value = groupFilteredTasks(condition, filteredTaskEntities)
                callback(true)
            }

            STAR -> {
                val starredTaskEntities = allTasksList
                    .flatMap { it.list }
                    .filter { it.starred }

                _filteredTaskList.value = groupFilteredTasks(condition, starredTaskEntities)
                callback(true)
            }

            CATEGORY -> {
                val categoryEntities = allTasksList
                    .flatMap { it.list }
                    .filter { task ->
                        // Check if the task's categoryIds contain the query
                        task.categoryIds.any { it.contains(query!!, ignoreCase = true) }
                    }
                val categoryName =
                    _categories.value?.find { it.categoryId == query }?.categoryName ?: CATEGORY

                _filteredTaskList.value = groupFilteredTasks(categoryName, categoryEntities)
                callback(true)
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

    fun setIsSelectionModeOn(isItemSelected: Boolean) {
        _isSelectionModeOn.value = isItemSelected
        if (!isItemSelected) {
            fillSelection(emptyList())
            startTasksListener()
        }
    }

    fun updateHighlightedListName(name: String) {
        _highlightedListName.value = name
    }

    fun toggleSelection(task: TaskEntity) {
        val highlightedList = _highlightedTaskList.value ?: emptyList()
        val currentList = mutableListOf<TaskEntity>()
        if (highlightedList.isNotEmpty()) currentList.addAll(highlightedList)

        if (currentList.contains(task)) {
            currentList.remove(task)
        } else {
            currentList.add(task)
        }

        fillSelection(currentList)

        // Set select mode state and update UI accordingly
        val isInSelectMode = currentList.isNotEmpty()

        setIsSelectionModeOn(isInSelectMode)
    }

    fun fillSelection(tasks: List<TaskEntity>) {
        _highlightedTaskList.value = tasks
    }

    fun clearSelection() {
        fillSelection(emptyList())
    }

    /**
     *-----------------------------------------------------------------------------------------------
     */

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

    /**
     * BackgroundTasks ------------------------------------------------------------------------------
     */

    private fun uploadTasksInBackground(user: UserEntity, newDataJson: String) {
        val data = Data.Builder()
            .putString(USERID, user.userId)
            .putString(NEWDATAJSON, newDataJson)
            .build()

        repository.enqueueUploadTasksWork(data, application.applicationContext)
    }

    private fun uploadCategoriesInBackground(userId: String, newDataJson: String) {
        val data = Data.Builder()
            .putString(USERID, userId)
            .putString(NEWDATAJSON, newDataJson)
            .build()

        repository.enqueueUploadCategoryWork(data, application.applicationContext)
    }

    private fun uploadAvatarInBackground(userId: String, avatarFilePath: String) {

        if (avatarFilePath != NULL) {
            val data = Data.Builder()
                .putString(USERID, userId)
                .putString(FILEPATH, avatarFilePath)
                .build()

            repository.enqueueUploadAvatarWork(data, application.applicationContext)
        }
    }

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

    /**
     *-----------------------------------------------------------------------------------------------
     */
    private fun getIsFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean("isFirstLaunch", true)
    }

    private fun getBiometricAuthStatus(): Boolean {
        return sharedPreferences.getBoolean("isBiometricEnabled", false)
    }

    private fun getEmailAuthStatus(): Boolean {
        return sharedPreferences.getBoolean("isEmailAuthSet", true)
    }

    private fun getConnectedAccounts() {
        _isGoogleConnected.value = sharedPreferences.getBoolean("isGoogleConnected", false)
        _isFacebookConnected.value = sharedPreferences.getBoolean("isFacebookConnected", false)
        _isTwitterConnected.value = sharedPreferences.getBoolean("isTwitterConnected", false)
    }

    fun getSortingCondition(): Pair<String, Boolean> {
        val sortBy = sharedPreferences.getString("sortBy", DATE)
        val order = sharedPreferences.getBoolean("order", true)
        return Pair(sortBy, order)
    }

    // Function to update the isFirstLaunch status
    fun updateFirstLaunchStatus(isFirstLaunch: Boolean) {
        _isFirstLaunch.value = isFirstLaunch

        // Store the updated isFirstLaunch status in SharedPreferences
        sharedPreferences.putBoolean("isFirstLaunch", isFirstLaunch)
    }

    fun updateIsBiometricEnabled(isBiometricEnabled: Boolean) {
        _isBiometricEnabled.value = isBiometricEnabled

        // Store the updated isBiometricEnabled status in SharedPreferences
        sharedPreferences.putBoolean("isBiometricEnabled", isBiometricEnabled)
    }

    fun updateAuthProviderState(emailAuthSet: Boolean) {
        _isEmailAuthSet.value = emailAuthSet

        // Store the updated isBiometricEnabled status in SharedPreferences
        sharedPreferences.putBoolean("isEmailAuthSet", emailAuthSet)
    }

    private fun updateAuthenticationData(email: String, pass: String) {
        // Store the updated authentication data in SharedPreferences
        sharedPreferences.putString("email", email)
        sharedPreferences.putString("pass", pass)
    }

    fun upDateConnectedAccount(account: Int, isConnected: Boolean) {

        when (account) {
            1 -> {
                sharedPreferences.putBoolean("isGoogleConnected", isConnected)
            }

            2 -> {
                sharedPreferences.putBoolean("isFacebookConnected", isConnected)
            }

            3 -> {
                sharedPreferences.putBoolean("isTwitterConnected", isConnected)
            }
        }
        getConnectedAccounts()
    }

    private fun updateAuthState() {
        val user = firebase.auth.currentUser
        user?.let {
            updateAuthProviderState(user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID })
        }
    }

    fun updateSortingCondition(sortBy: String, order: Boolean) {
        sharedPreferences.putString("sortBy", sortBy)
        sharedPreferences.putBoolean("order", order)
    }

    fun updateWebClientId(webClientId: String) {
        sharedPreferences.putString("webClientId", webClientId)
    }

    private fun fetchProviders() {
        firebase.fetchSignInMethodsForUser { providersList ->
            if (providersList != null) {
                sharedPreferences.putBoolean("isGoogleConnected", providersList.contains("google"))
                sharedPreferences.putBoolean(
                    "isFacebookConnected",
                    providersList.contains("facebook")
                )
                sharedPreferences.putBoolean(
                    "isTwitterConnected",
                    providersList.contains("twitter")
                )
            }
            getConnectedAccounts()
        }
    }

    fun logOut(callback: (Boolean, String?) -> Unit) {

        firebase.logOut { logOutSuccessful, errorMessage ->
            if (logOutSuccessful) {
                disconnectProviders()
                firebase.addLog("sign out successful")

                // Store reference to the current user's Id before returning
                currentUser.value?.userId?.let { repository.storeCurrentUserIdReference(it) }
                firebase.setUserId("")

                callback(true, null)
                resetLists()
            } else {
                firebase.addLog("sign out failed")
                callback(false, errorMessage)
            }
        }
    }

    private fun disconnectProviders() {
        val webClientId = sharedPreferences.getString("webClientId", "")

        if (webClientId != "") {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(application.applicationContext, gso)
            googleSignInClient.signOut()
        }
    }

    fun setSettingsItem(item: String) {
        _settingsItemSelected.value = item
    }

    private fun resetLists() {
        setIsSelectionModeOn(false)
        setIsSelectionModeOn(false)
        updateHighlightedListName("")
        setSettingsItem("")
        toggleSlider(false)
        fillSelection(emptyList())
        _selectedTaskCategories.value = emptyList()
        updateIsBiometricEnabled(false)
        updateAuthProviderState(true)
        updateAuthenticationData("", "")
    }

    /**
     *-----------------------------------------------------------------------------------------------
     */

    private fun convertTasksToJson(tasks: List<TaskEntity>): String {
        val gson = Gson()
        return gson.toJson(tasks)
    }

    private fun convertCategoriesToJson(categories: List<CategoryEntity>): String {
        val gson = Gson()
        return gson.toJson(categories)
    }

    fun toggleSlider(toClose: Boolean) {
        _closeSlider.value = toClose
    }

    /**
     *-----------------------------------------------------------------------------------------------
     */

    companion object {
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
        private const val CATEGORY = "category"
        private const val DATE = "date"
        private const val TITLE = "title"
    }
}