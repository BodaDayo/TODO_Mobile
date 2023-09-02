package com.rgbstudios.todomobile.data.repository

import android.content.ContentValues.TAG
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.rgbstudios.todomobile.data.entity.TaskEntity
import com.rgbstudios.todomobile.data.entity.UserEntity
import com.rgbstudios.todomobile.data.local.TaskDao
import com.rgbstudios.todomobile.data.local.UserDao
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import com.rgbstudios.todomobile.ui.AvatarManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
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

    private var userLoggedIn: Boolean = false
    private val userLoggedInLiveData = MutableLiveData<Boolean>()

    /* The MUTEX ensures that only one thread can execute a block of code enclosed within
     the withLock function at any given time, it helps to avoid potential race conditions that
     could lead to data inconsistencies or crashes
     */
    private val userSetupMutex = Mutex()

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

                userSetupMutex.withLock {

                    // Delete existing user and tasks
                    deleteUserDetailsFromDatabase()

                    // Save new user's details in local database
                    userDao.insertUser(newUser)

                    // Import new user's tasks
                    if (sender == "SignInFragment") {
                        importUserTasks(newUser.userId)
                    }

                    newUser
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up new user: ${e.message}")
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
                Log.e(TAG, "Error importing user details: ${e.message}", e)
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

                    Log.d("aaaaSnapshot", "gotten tasksList: $tasksToAdd")

                    // Insert all tasks into the local database
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            taskDao.insertAllTasks(tasksToAdd)
                            Log.d("aaaaInsert", "done, check")

                        } catch (e: Exception) {
                            Log.e(TAG, "Error inserting user details: ${e.message}")
                            throw e
                        }
                    }

                    Log.d("aaaaImport", "DONEEEEE!!!!!!!")
                }

                override fun onCancelled(e: DatabaseError) {
                    Log.e(TAG, "Downloading user's tasks cancelled: ${e.message}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error importing user's tasks: ${e.message}", e)
        }
    }

    private suspend fun deleteUserDetailsFromDatabase() {
        try {
            userDao.deleteUser()
            taskDao.deleteAllTasks()
        } catch (e: Exception) {

            Log.e(TAG, "Error deleting user details: ${e.message}", e)
        }
    }

    suspend fun saveTaskToDatabase(taskEntity: TaskEntity) {
        try {
            taskDao.insertTask(taskEntity)
        } catch (e: Exception) {

            Log.e(TAG, "Error saving task: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateTaskInDatabase(taskEntity: TaskEntity) {
        try {
            taskDao.updateTask(taskEntity)
        } catch (e: Exception) {

            Log.e(TAG, "Error updating task: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteTaskFromDatabase(taskId: String) {
        try {
            taskDao.deleteTask(taskId)
        } catch (e: Exception) {

            Log.e(TAG, "Error deleting task: ${e.message}", e)
            throw e
        }
    }

    suspend fun setPendingChangesFlag(flag: Boolean) {
        userDao.setTaskPendingChangesFlag(flag)
    }

    fun logout() {
        // Start sync work
        setUserLoggedInStatus(false)
    }

    // Method to toggle the user login flag
    private fun setUserLoggedInStatus(loggedIn: Boolean) {
        userLoggedIn = loggedIn
        userLoggedInLiveData.postValue(loggedIn)
    }

    fun observeHasPendingChanges(): Flow<Boolean> {
        return userDao.observeTaskPendingChanges()
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
                e.printStackTrace()
                return null
            }
        }

        // Create a directory to store avatars in the app's internal storage
        val avatarsDir = File(context.filesDir, "avatars")
        if (!avatarsDir.exists()) {
            avatarsDir.mkdirs()
        }

        val file = File(avatarsDir, "avatar.png")

        try {
            withContext(Dispatchers.IO) {
                val stream = FileOutputStream(file)
                avatarBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
                stream.close()
            }

            // Return the file path of the saved file
            return file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

}
