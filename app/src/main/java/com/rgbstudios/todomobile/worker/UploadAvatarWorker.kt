package com.rgbstudios.todomobile.worker

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rgbstudios.todomobile.data.entity.TaskEntity
import com.rgbstudios.todomobile.data.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class UploadAvatarWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val userJson = inputData.getString("user")

            // Deserialize userJson
            val user = userJson?.let { convertJsonToUser(it) }

            val storageReference =
                FirebaseStorage.getInstance().reference.child("avatars").child(user!!.userId)

            // Get the file path of the user's avatar from UserEntity
            val avatarFilePath = user.avatarFilePath

            if (avatarFilePath != null) {
                // Create a reference to the local avatar file
                val localFile = File(avatarFilePath)

                // Upload the local avatar file to FirebaseStorage
                storageReference.putFile(Uri.fromFile(localFile))

                Result.success()
            } else {
                Log.d(TAG, "avatarFilePath in the UploadAvatarWorker is null")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload work failed", e)
            Result.retry()
        }
    }

    private fun convertJsonToUser(json: String): UserEntity {
        val gson = Gson()
        val type = object : TypeToken<UserEntity>() {}.type
        return gson.fromJson(json, type)
    }
}