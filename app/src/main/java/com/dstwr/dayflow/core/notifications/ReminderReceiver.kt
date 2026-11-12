package com.dstwr.dayflow.core.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderChannels.create(context)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "لديك تذكير"
        val text = intent.getStringExtra(EXTRA_TEXT) ?: "حان الوقت لما خططت له."
        val notification = NotificationCompat.Builder(context, ReminderChannels.REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(intent.getIntExtra(EXTRA_ID, 1), notification)
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val EXTRA_ID = "id"
    }
}
