// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import com.xingheyuzhuan.shiguangschedule.data.sync.WidgetDataSynchronizer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate

class SchedulePreviewReceiver : BroadcastReceiver(), KoinComponent {
    private val settings: AppSettingsRepository by inject()
    private val courses: WidgetRepository by inject()
    private val synchronizer: WidgetDataSynchronizer by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != CourseNotificationWorker.ACTION_PREVIEW) return
        val date = intent.getStringExtra("date") ?: return
        val expected = LocalDate.now().plusDays(1).toString()
        if (date != expected) { CourseNotificationWorker.enqueue(context); return }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!settings.getAppSettingsOnce().schedulePreviewEnabled) return@launch
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return@launch
                val nm = context.getSystemService(NotificationManager::class.java)
                if (!nm.areNotificationsEnabled()) return@launch
                synchronizer.syncNow(notify = false)
                val tomorrow = courses.getWidgetCoursesByDateRange(date, date).first().filterNot { it.isSkipped }.sortedBy { it.startTime }
                if (tomorrow.isEmpty()) return@launch
                val channel = "hbmu_schedule_preview"
                nm.createNotificationChannel(NotificationChannel(channel, "明日课表预告", NotificationManager.IMPORTANCE_DEFAULT))
                val open = PendingIntent.getActivity(context, 50200, Intent(context, MainActivity::class.java).putExtra("open_week_schedule", true), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val style = NotificationCompat.InboxStyle().setBigContentTitle("明日课表 · ${tomorrow.size} 堂课")
                tomorrow.take(7).forEach { style.addLine("${it.startTime}  ${it.name} · ${it.position.ifBlank { "教室待公布" }}") }
                nm.notify(50200, NotificationCompat.Builder(context, channel).setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("明日课表 · ${tomorrow.size} 堂课")
                    .setContentText("首节 ${tomorrow.first().startTime} · ${tomorrow.first().name}")
                    .setStyle(style).setContentIntent(open).setAutoCancel(true).setTimeoutAfter(12 * 60 * 60 * 1000L)
                    .setCategory(NotificationCompat.CATEGORY_EVENT).build())
            } catch (e: Exception) {
                android.util.Log.w("SchedulePreview", "Unable to show schedule preview", e)
            } finally { pending.finish(); CourseNotificationWorker.enqueue(context) }
        }
    }
}

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { CourseNotificationWorker.enqueue(context) }
}
