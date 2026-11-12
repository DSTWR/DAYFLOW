package com.dstwr.dayflow.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object ReminderChannels {
    const val REMINDERS = "dayflow_reminders"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                REMINDERS,
                "تذكيرات DAYFLOW",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "تنبيهات المهام والمواعيد التي تختارها" }
            manager.createNotificationChannel(channel)
        }
    }
}
