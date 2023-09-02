package com.rgbstudios.todomobile

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.work.WorkManager
import com.rgbstudios.todomobile.data.local.TodoAppDatabase
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import com.rgbstudios.todomobile.data.repository.TodoRepository
import com.rgbstudios.todomobile.worker.SyncWorker
import com.rgbstudios.todomobile.worker.SyncWorkerFactory
import java.util.concurrent.TimeUnit

class TodoMobileApplication : Application() {

    val database: TodoAppDatabase by lazy { TodoAppDatabase.getDatabase(this) }

    val repository: TodoRepository by lazy {
        val taskDao = database.taskDao()
        val userDao = database.userDao()
        TodoRepository(taskDao, userDao)
    }

    override fun onCreate() {
        super.onCreate()

        /* / Create a Configuration object
        val configuration = Configuration.Builder()
            .setWorkerFactory(SyncWorkerFactory(repository))
            .build()

        /* / Set up one-time synchronization task using OneTimeWorkRequest
        val oneTimeSyncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        */

        // Set up periodic synchronization task to run every 30 minutes
        val periodicSyncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = 2,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        ).build()

        Log.d("aaaa4", "periodicSyncWorkRequest")
        // Enqueue the periodic synchronization task
        try {
            WorkManager.getInstance(applicationContext).enqueue(periodicSyncWorkRequest)
        } catch (e: Exception) {
            Log.d("aaaa5", "message: ${e.message}", e)
        }

        Log.d("aaaa4", "periodicSyncWorkRequest status: $periodicSyncWorkRequest")

        /* / Set up activity lifecycle callbacks
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityStarted(activity: Activity) {

                // Cancel all work
                WorkManager.getInstance(applicationContext).cancelAllWork()

                /* / Get the LifecycleOwner associated with the activity
                val lifecycleOwner = activity as? LifecycleOwner

                if (lifecycleOwner != null) {
                    // Remove the opposite task (one-time) when the app's state changes
                    val workInfoLiveData = WorkManager.getInstance(applicationContext)
                        .getWorkInfoByIdLiveData(oneTimeSyncWorkRequest.id)

                    workInfoLiveData.observe(lifecycleOwner) { workInfo ->
                        if (workInfo != null && !workInfo.state.isFinished) {
                            // Cancel the one-time synchronization task
                            WorkManager.getInstance(applicationContext)
                                .cancelWorkById(oneTimeSyncWorkRequest.id)
                        }
                    }
                }

                 */

                    // Listen for changes in user login status
                    repository.observeUserLoggedIn().observe(activity as LifecycleOwner) { loggedIn ->
                        if (loggedIn) {
                            // Enqueue the periodic synchronization task
                            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                                "OnlineSyncWork",
                                ExistingPeriodicWorkPolicy.KEEP,
                                periodicSyncWorkRequest
                            )
                        } else {
                            // Cancel all work when user is logged out
                            WorkManager.getInstance(applicationContext).cancelAllWork()
                        }
                    }


            }

            override fun onActivityResumed(activity: Activity) {
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {

                // Cancel the periodic synchronization task
                WorkManager.getInstance(applicationContext).cancelWorkById(periodicSyncWorkRequest.id)

                // Enqueue the one-time synchronization task if the user is logged in
                if (repository.isUserLoggedIn()) {
                    WorkManager.getInstance(applicationContext).enqueue(oneTimeSyncWorkRequest)
                }

            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
            }
        })
        */

         */
    }
}