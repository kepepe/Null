package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_ID = "study_sync_classes_v2"
    const val NOTIFICATION_ID_TEST = 1001

    sealed interface NotificationStatus {
        object Success : NotificationStatus
        object PermissionRequired : NotificationStatus
        object NotificationsDisabledInSystem : NotificationStatus
        data class Error(val message: String) : NotificationStatus
    }

    fun hasPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = "Напоминания о начале лекций и семинаров"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showClassReminderNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = NOTIFICATION_ID_TEST
    ): NotificationStatus {
        try {
            createNotificationChannel(context)

            val managerCompat = NotificationManagerCompat.from(context)
            if (!managerCompat.areNotificationsEnabled()) {
                return NotificationStatus.NotificationsDisabledInSystem
            }

            if (!hasPermission(context)) {
                return NotificationStatus.PermissionRequired
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_schedule)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            managerCompat.notify(notificationId, builder.build())
            return NotificationStatus.Success
        } catch (e: SecurityException) {
            return NotificationStatus.PermissionRequired
        } catch (e: Exception) {
            return NotificationStatus.Error(e.message ?: "Неизвестная ошибка")
        }
    }
}
