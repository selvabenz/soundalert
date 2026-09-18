package com.bridgeconn.soundalert

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(MainActivity.KEY_ENABLED, false)) return

        // Android does not allow a microphone foreground service to be silently started from boot.
        // We post a tiny resume reminder instead; tapping it opens the visible activity, which resumes listening.
        try {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.resume_channel), NotificationManager.IMPORTANCE_DEFAULT)
            )
            val openIntent = Intent(context, MainActivity::class.java)
            val pending = PendingIntent.getActivity(
                context,
                8,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            manager.notify(
                ID,
                Notification.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("SoundAlert ⏻")
                    .setContentText("●")
                    .setAutoCancel(true)
                    .setContentIntent(pending)
                    .build()
            )
        } catch (_: Throwable) { }
    }

    companion object {
        private const val CHANNEL = "soundalert_resume"
        private const val ID = 42022
    }
}
