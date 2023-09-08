package com.rgbstudios.todomobile.data.repository

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.rgbstudios.todomobile.data.entity.TaskEntity
import com.rgbstudios.todomobile.data.entity.UserEntity
import com.rgbstudios.todomobile.data.local.TaskDao
import com.rgbstudios.todomobile.data.local.UserDao
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import com.rgbstudios.todomobile.ui.AvatarManager
import com.rgbstudios.todomobile.worker.UploadAvatarWorker
import com.rgbstudios.todomobile.worker.UploadTasksWorker
import com.rgbstudios.todomobile.worker.UploadUserDetailsWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class TodoRepository(private val taskDao: TaskDao, private val userDao: UserDao) {

    private var currentUserId: String? = null

    /* The MUTEX ensures that only one thread can execute a block of code enclosed within
     the withLock function at any given time, it helps to avoid potential race conditions that
     could lead to data inconsistencies or crashes
     */
    private val userSetupMutex = Mutex()
    private val firebase = FirebaseAccess()

    fun getUserFromLocalDatabase(): Flow<UserEntity> {
        return userDao.getUser()
    }

    fun getTasksFromLocalDatabase(): Flow<List<TaskEntity>> {
        return taskDao.getAllTasks()
    }

    suspend fun setUpNewUserInDatabase(
        userId: String,
        email: String,
        userAvatarData: String?,
        sender: String
    ): UserEntity {
        return withContext(Dispatchers.IO) {
            try {

                // Determine name and occupation based on sender
                val (name, occupation) = when (sender) {
                    "SignInFragment" -> {
                        val userDetails = importUserDetails(userId)
                        Pair(userDetails?.getOrNull(0), userDetails?.getOrNull(1))
                    }

                    else -> Pair(null, null)
                }

                // Create new use Entity
                val newUser = UserEntity(
                    userId = userId,
                    name = name,
                    email = email,
                    occupation = occupation,
                    avatarFilePath = userAvatarData
                )

                // Check if user is re-signing in
                if (sender == "SignInFragment" && (currentUserId == null || newUser.userId != currentUserId)) {
                    userSetupMutex.withLock {
                        // Delete existing user and tasks
                        deleteUserDetailsFromDatabase()

                        // Save new user's details in the local database
                        userDao.insertUser(newUser)
                        Log.d(TAG, "new user: ${newUser.userId} inserted in database")

                        // Import new user's tasks
                        importUserTasks(newUser.userId)
                        Log.d(TAG, "new user: ${newUser.userId} tasks imported in database")
                    }
                } else {

                    userSetupMutex.withLock {
                        // Delete existing user and tasks
                        deleteUserDetailsFromDatabase()

                        // Save new user's details in local database
                        userDao.insertUser(newUser)
                        Log.d(TAG, "new user: ${newUser.userId} inserted in database")
                    }

                }
                newUser
            } catch (e: Exception) {
                throw e
            }
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

    private fun importUserTasks(userId: String) {
        try {
            val databaseListRef = FirebaseAccess().getTasksListRef(userId)

            databaseListRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tasksToAdd = mutableListOf<TaskEntity>()

                    for (item in snapshot.children) {
                        val taskIdFromFirebase = item.child("taskId").getValue(String::class.java)
                        val taskId = taskIdFromFirebase ?: UUID.randomUUID().toString()

                        val title = item.child("title").getValue(String::class.java) ?: ""
                        val description =
                            item.child("description").getValue(String::class.java) ?: ""
                        val taskCompleted =
                            item.child("taskCompleted").getValue(Boolean::class.java) ?: false
                        val starred = item.child("starred").getValue(Boolean::class.java) ?: false

                        val taskEntity =
                            TaskEntity(taskId, title, description, taskCompleted, starred)
                        tasksToAdd.add(taskEntity)
                    }

                    // Insert all tasks into the local database
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            taskDao.insertAllTasks(tasksToAdd)

                        } catch (e: Exception) {
                            throw e
                        }
                    }
                }

                override fun onCancelled(e: DatabaseError) {
                    Log.e(TAG, "Downloading user's tasks cancelled: ${e.message}")
                }
            })
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
        }
    }

    private suspend fun deleteUserDetailsFromDatabase() {
        try {
            userDao.deleteUser()
            taskDao.deleteAllTasks()
        } catch (e: Exception) {
            firebase.recordCaughtException(e)
        }
    }

    suspend fun saveTaskToDatabase(taskEntity: TaskEntity) {
        try {
            taskDao.insertTask(taskEntity)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateTaskInDatabase(taskEntity: TaskEntity) {
        try {
            taskDao.updateTask(taskEntity)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteTaskFromDatabase(taskId: String) {
        try {
            taskDao.deleteTask(taskId)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getAvatar(
        userId: String,
        sender: String,
        context: Context,
        resources: Resources
    ): String? {
        val avatarBitmap: Bitmap = if (sender == "SignUpFragment") {
            // Get the drawable resource ID of a random avatar
            val avatarResource = AvatarManager().defaultAvatar

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


    fun saveCurrentUserId(userId: String) {
        currentUserId = userId
    }

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
                "uploadUserTasks",
                ExistingWorkPolicy.REPLACE,
                uploadWorkRequest
            )
    }

    suspend fun updateUserInDatabase(updatedUser: UserEntity) {
        try {
            userDao.updateUser(updatedUser)
            firebase.addLog("userDetails update in room successful")
        } catch (e: Exception) {
            throw e
        }
    }

    companion object {
        private const val TAG = "TodoRepository"
    }

}
