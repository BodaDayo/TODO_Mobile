package com.rgbstudios.todomobile.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rgbstudios.todomobile.R
import com.rgbstudios.todomobile.ui.activities.MainActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val taskId = intent?.getStringExtra("taskId")
        val title = intent?.getStringExtra("title")
        val interval = intent?.getStringExtra("interval")

        if (context != null && taskId != null && title != null) {
            // Use taskId and title as channel ID and name

            // Create an intent to launch when the notification is clicked
            val resultIntent = Intent(context, MainActivity::class.java)
            // Add any necessary extras to the intent
            resultIntent.putExtra("taskId", taskId)

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Create the notification
            val notificationBuilder = NotificationCompat.Builder(context, taskId)
                .setContentTitle("Task Due Soon")
                .setContentText("Your task '$title' is due $interval.")
                .setSmallIcon(R.drawable.notifications_none) // Replace with your notification icon
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            // Show the notification
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    taskId,
                    title,
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            notificationManager.notify(1, notificationBuilder.build())
        }
    }
}

