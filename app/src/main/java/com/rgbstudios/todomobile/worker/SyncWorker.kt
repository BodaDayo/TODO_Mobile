package com.rgbstudios.todomobile.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rgbstudios.todomobile.data.repository.TodoRepository

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val repository: TodoRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        /*try {
            // Fetch the user entity from the database using a suspend function
            val userEntity = repository.getUserFromLocalDatabase() ?: return Result.retry()
            Log.d("aaaa", "userId: ${userEntity.userId}, email: ${userEntity.email}")

            if (userEntity.hasPendingChanges) {
                Log.d("aaaa2", "changes?: ${userEntity.hasPendingChanges}, email: ${userEntity.email}")
                // Check network connectivity
                if (isNetworkAvailable()) {
                    Log.d("aaaa3", "network okay")
                    // Perform synchronization logic here
                    performSynchronization(userEntity.userId)

                } else {
                    // Handle no network connectivity, perhaps by scheduling a retry
                    return Result.retry()
                }
            }

            return Result.success()
        } catch (e: Exception) {
            Log.d("aaaa", "message: ${e.message}", e)
            return Result.failure()
        }*/
        return Result.success()
    }

    private fun isNetworkAvailable(): Boolean {
        // Get the ConnectivityManager instance
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Get the network capabilities
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        // Check if the network capabilities have internet access
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    }

    private suspend fun performSynchronization(userId: String) {
        Log.d("aaaa4", "Synchronization entered")
        //repository.prepareTasksForUpload(userId)
    }
}
