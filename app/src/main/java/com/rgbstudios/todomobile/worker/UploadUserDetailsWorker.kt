package com.rgbstudios.todomobile.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UploadUserDetailsWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val userId = inputData.getString("userId")
            val name = inputData.getString("name")
            val occupation = inputData.getString("occupation")
            val avatarFilePath = inputData.getString("avatarFilePath")

            if (userId != null) {
                val destinationRef = FirebaseAccess().getUserDetailsRef(userId)
                val userData = mapOf(
                    "name" to name,
                    "occupation" to occupation,
                    "avatarFilePath" to avatarFilePath
                )
                destinationRef.setValue(userData)
                Result.success()
            } else {
                Log.d(TAG, "userId in the UploadUserDetailsWorker is null")
                Result.failure()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Upload work failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "UploadUserDetailsWorker"
    }
}