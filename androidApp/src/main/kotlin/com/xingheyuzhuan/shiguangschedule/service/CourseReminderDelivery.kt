package com.xingheyuzhuan.shiguangschedule.service

import android.content.Context
import android.util.Log
import com.xingheyuzhuan.shiguangschedule.notifications.CourseReminder
import com.xingheyuzhuan.shiguangschedule.notifications.ReminderDeliveryGate
import com.xingheyuzhuan.shiguangschedule.notifications.ReminderDeliveryStore
import java.security.MessageDigest

internal object CourseReminderDelivery {
    @Volatile private var instance: ReminderDeliveryGate? = null

    fun gate(context: Context): ReminderDeliveryGate = instance ?: synchronized(this) {
        instance ?: create(context.applicationContext).also { instance = it }
    }

    fun key(event: CourseReminder): String = MessageDigest.getInstance("SHA-256")
        .digest(event.occurrenceKey.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun create(context: Context): ReminderDeliveryGate {
        // Store only a one-way occurrence digest and its expiration, not course/account data.
        val prefs = context.getSharedPreferences("course_reminder_deliveries_v1", Context.MODE_PRIVATE)
        return ReminderDeliveryGate(object : ReminderDeliveryStore {
            override fun read(): Map<String, Long> = prefs.all.mapNotNull { (key, value) ->
                (value as? Long)?.let { key to it }
            }.toMap()

            override fun write(deliveries: Map<String, Long>): Boolean {
                val editor = prefs.edit().clear()
                deliveries.forEach { (key, expiry) -> editor.putLong(key, expiry) }
                return editor.commit().also { if (!it) Log.w("CourseReminders", "Unable to persist reminder delivery") }
            }
        })
    }
}
