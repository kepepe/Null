package com.example.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Log

object SilentModeHelper {

    private const val TAG = "SilentModeHelper"

    fun isDndAccessGranted(context: Context): Boolean {
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.isNotificationPolicyAccessGranted ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun openDndSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open DND settings: ${e.message}")
        }
    }

    /**
     * Applies true silent mode (ringer off, vibration off).
     * When DND permission is granted, sets INTERRUPTION_FILTER_ALARMS or INTERRUPTION_FILTER_NONE
     * and ringerMode = RINGER_MODE_SILENT.
     */
    fun applyClassSilentMode(context: Context, enableSilent: Boolean): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            val hasDnd = nm?.isNotificationPolicyAccessGranted ?: false

            if (enableSilent) {
                if (hasDnd) {
                    // Set DND filter to alarms only so phone stays completely silent (no ring, no vibrate)
                    nm?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    runCatching {
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
                    }
                    true
                } else {
                    // Direct silent mode switch
                    runCatching {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
                    }.isSuccess
                }
            } else {
                if (hasDnd) {
                    nm?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    }
                    true
                } else {
                    runCatching {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    }.isSuccess
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice setting silent mode: ${e.message}")
            false
        }
    }
}
