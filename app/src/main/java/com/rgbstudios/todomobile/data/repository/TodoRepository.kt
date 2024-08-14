package com.rgbstudios.todomobile.data.repository

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.google.firebase.database.GenericTypeIndicator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.data.entity.CategoryEntity
import com.rgbstudios.todomobile.data.entity.TaskEntity
import com.rgbstudios.todomobile.data.entity.UserEntity
import com.rgbstudios.todomobile.data.local.CategoryDao
import com.rgbstudios.todomobile.data.local.TaskDao
import com.rgbstudios.todomobile.data.local.UserDao
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import com.rgbstudios.todomobile.utils.AvatarManager
import com.rgbstudios.todomobile.utils.CategoryManager
import com.rgbstudios.todomobile.worker.SetAlarmWorker
import com.rgbstudios.todomobile.worker.UploadAvatarWorker
import com.rgbstudios.todomobile.worker.UploadCategoriesWorker
import com.rgbstudios.todomobile.worker.UploadTasksWorker
import com.rgbstudios.todomobile.worker.UploadUserDetailsWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class TodoRepository(
    private val taskDao: TaskDao,
    private val userDao: UserDao,
    private val categoryDao: CategoryDao
) {

    private var currentUserId: String? = null
    private val defaultCategories = CategoryManager()
    private val avatars = AvatarManager()

    /* The MUTEX ensures that only one thread can execute a block of code enclosed within
     the withLock function at any given time, it helps to avoid potential race conditions that
     could lead to data inconsistencies or crashes
     */
    private val userSetupMutex = Mutex()
    private val firebase = FirebaseAccess()

    /**
     *-----------------------------------------------------------------------------------------------
     */

    fun getUserFromLocalDB(): Flow<UserEntity> {
        return userDao.getUser()
    }

    fun getTasksFromLocalDB(): Flow<List<TaskEntity>> {
        return taskDao.getAllTasks()
    }

    fun getCategoriesFromDB(): Flow<List<CategoryEntity>> {
        return categoryDao.getCategories()
    }

    /**
     *-----------------------------------------------------------------------------------------------
     */

    suspend fun setUpUserInDB(
        userId: String,
        email: String,
        userAvatarData: String?,
        sender: String,
        nameFromAuth: String?
    ): Pair<UserEntity, List<CategoryEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                val (name, occupation) = determineNameAndOccupation(sender, nameFromAuth, userId)
                val newUser = UserEntity(
                    userId = userId,
                    name = name,
                    email = email,
                    occupation = occupation,
                    avatarFilePath = userAvatarData
                )
                val defaultCategories = defaultCategories.getDefaultCategories()
                when (sender) {
                    SIGNIN -> handleSignIn(newUser, defaultCategories)
                    SIGNUP -> handleSignUp(newUser, defaultCategories)
                    else -> handleOtherCases(newUser)
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    private suspend fun determineNameAndOccupation(sender: String, nameFromAuth: String?, userId: String): Pair<String?, String?> {
        return when {
            nameFromAuth != null -> Pair(nameFromAuth, null)
            sender == "SignInFragment" -> {
                val userDetails = importUserDetails(userId)
                Pair(userDetails?.getOrNull(0), userDetails?.getOrNull(1))
            }
            else -> Pair(null, null)
        }
    }

    private suspend fun handleSignIn(newUser: UserEntity, defaultCategories: List<CategoryEntity>): Pair<UserEntity, List<CategoryEntity>> {
        userSetupMutex.withLock {
            deleteUserDetailsFromDB()
            userDao.insertUser(newUser)
            val importedTasks = importUserTasks(newUser.userId)
            importedTasks?.let { taskDao.insertAllTasks(it) }
            val importedCategories = importCategories(newUser.userId)
            val finalCategories = if (importedCategories != null) {
                val deserializedCategories = convertJsonToCategories(importedCategories)
                categoryDao.insertAllCategories(deserializedCategories)
                deserializedCategories
            } else {
                categoryDao.insertAllCategories(defaultCategories)
                defaultCategories
            }
            return Pair(newUser, finalCategories)
        }
    }

    private suspend fun handleSignUp(newUser: UserEntity, defaultCategories: List<CategoryEntity>): Pair<UserEntity, List<CategoryEntity>> {
        userSetupMutex.withLock {
            deleteUserDetailsFromDB()
            userDao.insertUser(newUser)
            categoryDao.insertAllCategories(defaultCategories)
            return Pair(newUser, defaultCategories)
        }
    }

    private suspend fun handleOtherCases(newUser: UserEntity): Pair<UserEntity, List<CategoryEntity>> {
        userSetupMutex.withLock {
            var currentCategories: List<CategoryEntity>? = null
            categoryDao.getCategories().collect { currentCategories = it }
            val categories = currentCategories ?: defaultCategories.getDefaultCategories()
            return Pair(newUser, categories)
        }
    }

    private suspend fun importUserDetails(userId: String): List<String>? {
        return withContext(Dispatchers.IO) {
            try {
                val userDetailsRef = FirebaseAccess().getUserDetailsRef(userId)

                val deferred = async {
                    val snapshot = userDetailsRef.get().await()
                    val name = snapshot.child("name").getValue(String::class.java)
                    val occupation = snapshot.child("occupation").getValue(String::class.java)

                    if (name != null && occupation != null) {
                        listOf(name, occupation)
                    } else {
                        null
                    }
                }

                // Wait for the async block to complete
                deferred.await()
            } catch (e: Exception) {
                firebase.recordCaughtException(e)
                null
            }
        }
    }

    private suspend fun importCategories(userId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val categoryListRef = FirebaseAccess().getCategoriesListRef(userId)

                val deferred = async {
                    val snapshot = categoryListRef.get().await()
                    val categories = snapshot.getValue(String::class.java)
                    categories
                }
                // Wait for the async block to complete
                deferred.await()
            } catch (e: Exception) {
                firebase.recordCaughtException(e)
                null
            }
        }
    }

    private suspend fun importUserTasks(userId: String): List<TaskEntity>? {
        return withContext(Dispatchers.IO) {
            try {
                val tasksListRef = FirebaseAccess().getTasksListRef(userId)

                val deferred = async {
                    val snapshot = tasksListRef.get().await()
                    val tasksHashMap =
                        snapshot.getValue(object : GenericTypeIndicator<HashMap<String, Any>>() {})

                    if (tasksHashMap != null) {
                        val taskList = mutableListOf<TaskEntity>()

                        for ((_, taskDataJson) in tasksHashMap) {
                            try {
                                val taskData = convertJsonToTask(taskDataJson as String)
                                // Add the parsed TaskEntity to the list
                                taskList.add(taskData)
                            } catch (e: Exception) {
                                firebase.recordCaughtException(e)
                            }
                        }
                        taskList
                    } else {
                        null
                    }
                }
                // Wait for the async block to complete
                deferred.await()
            } catch (e: Exception) {
                firebase.recordCaughtException(e)
                null
            }
        }
    }

    private suspend fun deleteUserDetailsFromDB() {
        try {
            userDao.deleteUser()
            taskDao.deleteAllTasks()
            categoryDao.deleteAllCategories()
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
        }
    }

    /**
     *-----------------------------------------------------------------------------------------------
     */

    suspend fun saveTaskToDB(taskEntity: TaskEntity) {
        try {
            taskDao.insertTask(taskEntity)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun batchSaveTaskToDB(taskEntityList: List<TaskEntity>) {
        try {
            taskDao.insertAllTasks(taskEntityList)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateTaskInDB(taskEntity: TaskEntity) {
        try {
            taskDao.updateTask(taskEntity)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteTaskFromDB(taskId: String) {
        try {
            taskDao.deleteTask(taskId)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteMultipleTasksFromDB(taskIdList: List<String>) {
        try {
            taskDao.deleteTasksByIds(taskIdList)
        } catch (e: Exception) {
            throw e
        }
    }

    private fun convertJsonToTask(json: String): TaskEntity {
        val gson = Gson()
        val type = object : TypeToken<TaskEntity>() {}.type
        return gson.fromJson(json, type)
    }

    /**
     *-----------------------------------------------------------------------------------------------
     */

    suspend fun getAvatar(
        userId: String,
        sender: String,
        context: Context,
        resources: Resources,
        uriFromAuth: Uri?
    ): String? {

        val avatarBitmap: Bitmap = if (uriFromAuth != null) {
            try {
                // Convert the URI to a Bitmap using Glide
                withContext(Dispatchers.IO) {
                    Glide.with(context)
                        .asBitmap()
                        .load(uriFromAuth)
                        .submit()
                        .get()
                }
            } catch (e: Exception) {
                firebase.recordCaughtException(e)

                val avatarResource = avatars.defaultAvatar
                // Convert the drawable resource to a bitmap
                BitmapFactory.decodeResource(resources, avatarResource)
            }
        } else if (sender == SIGNUP) {
            // Get the drawable resource ID of a random avatar
            val avatarResource = avatars.defaultAvatar

            // Convert the drawable resource to a bitmap
            BitmapFactory.decodeResource(resources, avatarResource)
        } else {
            try {
                val uri = FirebaseAccess().getAvatarStorageRef(userId).downloadUrl.await()

                // Convert the URI to a Bitmap using Glide
                withContext(Dispatchers.IO) {
                    Glide.with(context)
                        .asBitmap()
                        .load(uri)
                        .submit()
                        .get()
                }
            } catch (e: Exception) {
                firebase.recordCaughtException(e)
                return null
            }
        }

        // Call the saveAvatarBitmapToFile function to save the avatar to a file
        return saveAvatarBitmapToFile(userId, context, avatarBitmap)
    }

    suspend fun saveAvatarBitmapToFile(
        userId: String,
        context: Context,
        avatarBitmap: Bitmap
    ): String? {
        // Generate a unique file name based on the username and timestamp
        val timestamp = System.currentTimeMillis()
        val uniqueFileName = "$userId-avatar-$timestamp.png"

        // Create a directory to store avatars in the app's internal storage
        val avatarsDir = File(context.filesDir, "avatars")
        if (!avatarsDir.exists()) {
            avatarsDir.mkdirs()
        }

        // Delete the old avatar file if it exists in the directory
        val oldAvatarFile = File(avatarsDir, "*.png")
        if (oldAvatarFile.exists()) {
            oldAvatarFile.delete()
        }

        val file = File(avatarsDir, uniqueFileName)

        return try {
            withContext(Dispatchers.IO) {
                val stream = FileOutputStream(file)
                avatarBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
                stream.close()
            }

            // Return the file path of the saved file
            file.absolutePath
        } catch (e: IOException) {
            firebase.recordCaughtException(e)
            null
        }
    }

    /**
     *-----------------------------------------------------------------------------------------------
     */

    fun enqueueUploadUserDetailsWork(data: Data, context: Context) {

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Requires network connection
            .build()

        val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadUserDetailsWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "uploadUserDetails",
                ExistingWorkPolicy.REPLACE,
                uploadWorkRequest
            )
    }

    fun enqueueUploadCategoryWork(data: Data, context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Requires network connection
            .build()

        val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadCategoriesWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "uploadUserCategories",
                ExistingWorkPolicy.REPLACE,
                uploadWorkRequest
            )
    }

    fun enqueueUploadAvatarWork(data: Data, context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Requires network connection
            .build()

        val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadAvatarWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "uploadUserAvatar",
                ExistingWorkPolicy.REPLACE,
                uploadWorkRequest
            )
    }

    fun enqueueSetupAlarmWork(data: Data, context: Context) {

        val setUpAlarmWorkRequest = OneTimeWorkRequestBuilder<SetAlarmWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                context.getString(R.string.setupreminders),
                ExistingWorkPolicy.REPLACE,
                setUpAlarmWorkRequest
            )
    }

    fun enqueueUploadTasksWork(data: Data, context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Requires network connection
            .build()

        val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadTasksWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                context.getString(R.string.uploadusertasks),
                ExistingWorkPolicy.REPLACE,
                uploadWorkRequest
            )
    }

    /**
     *-----------------------------------------------------------------------------------------------
     */

    suspend fun updateUserInDB(updatedUser: UserEntity) {
        try {
            userDao.updateUser(updatedUser)
            firebase.addLog("userDetails update in room successful")
        } catch (e: Exception) {
            throw e
        }
    }

    fun storeCurrentUserIdReference(userId: String) {
        currentUserId = userId
    }

    /**
     *-----------------------------------------------------------------------------------------------
     */

    suspend fun saveCategoryToDB(categoryEntity: CategoryEntity) {
        categoryDao.insertCategory(categoryEntity)
    }

    suspend fun updateCategoryInDB(categoryEntity: CategoryEntity) {
        try {
            categoryDao.updateCategory(categoryEntity)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteCategoryFromDB(categoryId: String) {
        try {
            categoryDao.deleteCategory(categoryId)
        } catch (e: Exception) {
            throw e
        }
    }

    private fun convertJsonToCategories(json: String): List<CategoryEntity> {
        val gson = Gson()
        val type = object : TypeToken<List<CategoryEntity>>() {}.type
        return gson.fromJson(json, type)
    }

    /**
     *-----------------------------------------------------------------------------------------------
     */

    companion object {
        private const val SIGNUP = "SignUpFragment"
        private const val SIGNIN = "SignInFragment"
    }

}
