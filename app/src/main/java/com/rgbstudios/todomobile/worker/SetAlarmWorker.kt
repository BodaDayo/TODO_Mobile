package com.rgbstudios.todomobile.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rgbstudios.todomobile.data.entity.TaskEntity
import com.rgbstudios.todomobile.utils.AlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SetAlarmWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val taskDataJson = inputData.getString("tasksData")

            // Deserialize tasksDataJson
            val tasks = taskDataJson?.let { convertJsonToTasks(it) }

            if (tasks != null) {
                for (task in tasks) {
                    // Check if dueDateTime is not null and in the future
                    if (task.dueDateTime != null && task.dueDateTime.timeInMillis > System.currentTimeMillis()) {
                        // Calculate the alarm times for 1 day, 1 hour, and 5 minutes before the due time
                        val oneDayBefore = task.dueDateTime.timeInMillis - (1 * 24 * 60 * 60 * 1000)
                        val oneHourBefore = task.dueDateTime.timeInMillis - (1 * 60 * 60 * 1000)
                        val fiveMinutesBefore = task.dueDateTime.timeInMillis - (5 * 60 * 1000)

                        // Check if each interval is valid before setting the alarm
                        if (oneDayBefore > System.currentTimeMillis()) {
                            setUpAlarm(applicationContext, task.taskId, oneDayBefore, task.title, "tomorrow")
                        }

                        if (oneHourBefore > System.currentTimeMillis()) {
                            setUpAlarm(applicationContext, task.taskId, oneHourBefore, task.title, "in 1 Hour")
                        }

                        if (fiveMinutesBefore > System.currentTimeMillis()) {
                            setUpAlarm(applicationContext, task.taskId, fiveMinutesBefore, task.title, "in 5 Minutes")
                        }
                    }

                }
                Result.success()
            } else {
                Log.d(TAG, "tasks in the SetAlarmWorker is null")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Set Alarm work failed", e)
            Result.retry()
        }
    }

    private fun setUpAlarm(context: Context, taskId: String, alarmTime: Long, title: String, interval: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("taskId", taskId)
            putExtra("title", title)
            putExtra("interval", interval)
        }

        // Include taskId and interval in the request code to make each PendingIntent unique
        val requestCode = (taskId + interval).hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            alarmTime,
            pendingIntent
        )
    }

    private fun convertJsonToTasks(json: String): List<TaskEntity> {
        val gson = Gson()
        val type = object : TypeToken<List<TaskEntity>>() {}.type
        return gson.fromJson(json, type)
    }

    companion object {
        private const val TAG = "SetAlarmWorker"
    }
}

