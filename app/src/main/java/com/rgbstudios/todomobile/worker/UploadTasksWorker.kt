package com.rgbstudios.todomobile.worker

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rgbstudios.todomobile.data.entity.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UploadTasksWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val userId = inputData.getString("userId")
            val newDataJson = inputData.getString("newDataJson")

            // Deserialize newDataJson using your preferred method (e.g., Gson)
            val newDataList = newDataJson?.let { convertJsonToTasks(it) }

            val destinationRef = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(userId!!)
                .child("tasks")

            if (newDataList != null) {
                for (task in newDataList) {
                    val taskData = mapOf(
                        "taskId" to task.taskId,
                        "title" to task.title,
                        "description" to task.description,
                        "taskCompleted" to task.taskCompleted,
                        "starred" to task.starred
                    )

                    // Upload the task data to Firebase
                    destinationRef.child(task.taskId).setValue(taskData)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Task upload work failed", e)
            Result.retry()
        }
    }

    private fun convertJsonToTasks(json: String): List<TaskEntity> {
        val gson = Gson()
        val type = object : TypeToken<List<TaskEntity>>() {}.type
        return gson.fromJson(json, type)
    }

}
