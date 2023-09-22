package com.rgbstudios.todomobile.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rgbstudios.todomobile.data.remote.FirebaseAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UploadCategoriesWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val userId = inputData.getString("userId")
            val newDataList = inputData.getString("newDataJson")

            if (userId != null) {
                val destinationRef = FirebaseAccess().getCategoriesListRef(userId)

                if (newDataList != null) {

                    // Upload the category list data to Firebase
                    destinationRef.setValue(newDataList)

                    Result.success()
                } else {
                    Log.d(TAG, "categoryList in the $TAG is null")
                    Result.failure()
                }
            } else {
                Log.d(TAG, "userId in the $TAG is null")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Category upload work failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "UploadCategoriesWorker"
    }
}