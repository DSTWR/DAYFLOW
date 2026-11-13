package com.dstwr.dayflow.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val saved = context.getSharedPreferences("dayflow_local", Context.MODE_PRIVATE)
            .getString("reminders", "[]") ?: "[]"
        val items = try { JSONArray(saved) } catch (_: Exception) { JSONArray() }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val id = item.optLong("id")
            val title = item.optString("title")
            val details = item.optString("detail")
            val trigger = try {
                LocalDateTime.parse(details.take(16), formatter)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) { continue }
            if (trigger > System.currentTimeMillis()) {
                ReminderScheduler.schedule(context, id.toInt(), title, details.drop(16).trim().ifBlank { "حان موعد تذكيرك" }, trigger)
            }
        }
    }
}
