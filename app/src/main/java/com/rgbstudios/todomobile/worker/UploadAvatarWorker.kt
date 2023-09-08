package com.rgbstudios.todomobile.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class UploadAvatarWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val userId = inputData.getString("userId")
            val avatarFilePath = inputData.getString("avatarFilePath")

            if (userId != null) {
                val storageReference = FirebaseAccess().getAvatarStorageRef(userId)

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
            } else {
                Log.d(TAG, "deserialized user in the UploadAvatarWorker is null")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload work failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "UploadAvatarWorker"
    }
}