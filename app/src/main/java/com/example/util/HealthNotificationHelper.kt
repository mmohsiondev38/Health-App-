package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class HealthNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "health_milestones_channel"
        const val CHANNEL_NAME = "Daily Milestones & Goals"
        const val CHANNEL_DESC = "Notifications for walking goals, caloric consumption limits, and weight milestones."
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun postNotification(title: String, message: String, notificationId: Int = System.currentTimeMillis().toInt()) {
        // Double check permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // If permission is denied, fallback to in-app notifier or ignore.
                return
            }
        }

        // We use Android's default notification style with an ic_launcher_foreground icon.
        // It creates a gorgeous adaptive icon in the app shade.
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback standard system drawable that always exists
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
