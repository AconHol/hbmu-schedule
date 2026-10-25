// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import com.xingheyuzhuan.shiguangschedule.data.sync.WidgetDataSynchronizer
import com.xingheyuzhuan.shiguangschedule.notifications.ReminderPlanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import org.koin.android.annotation.KoinWorker
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

@KoinWorker
class CourseNotificationWorker(
    context: Context, params: WorkerParameters,
    private val settingsRepository: AppSettingsRepository,
    private val widgetRepository: WidgetRepository,
    private val synchronizer: WidgetDataSynchronizer
) : CoroutineWorker(context, params) {
    private val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun doWork(): Result = schedulingMutex.withLock {
        try { rebuildAlarms() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { Log.e("CourseNotifications", "Unable to schedule course notifications", e); Result.retry() }
    }

    private suspend fun rebuildAlarms(): Result {
        val settings = settingsRepository.getAppSettingsOnce()
        if ((settings.reminderEnabled || settings.schedulePreviewEnabled) && NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            // Rebuild date-specific data on boot and periodic refresh, even if the app was closed.
            synchronizer.syncNow(notify = false)
            val today = LocalDate.now()
            val courses = widgetRepository.getWidgetCoursesByDateRange(today.toString(), today.plusDays(7).toString()).first()
            val now = Clock.System.now()
            val zone = TimeZone.currentSystemDefault()
            val deliveries = CourseReminderDelivery.gate(applicationContext)
            val reminders = if (settings.reminderEnabled) ReminderPlanner.courses(courses, now, zone, settings.remindBeforeMinutes)
                .filterNot { deliveries.wasDelivered(CourseReminderDelivery.key(it), now.toEpochMilliseconds()) }.take(101) else emptyList()
            val previews = if (settings.schedulePreviewEnabled) ReminderPlanner.previews(courses, now, zone, settings.schedulePreviewHour).take(7) else emptyList()
            // Periodic refresh and foreground/sync refresh use different WorkManager names.
            // Serialize their complete cancel/register phase and stop replaced workers promptly.
            currentCoroutineContext().ensureActive()
            cancelAllSlots()
            reminders.forEachIndexed { index, event ->
                currentCoroutineContext().ensureActive()
                val slot = 50010 + index
                val intent = Intent(applicationContext, CourseAlarmReceiver::class.java).apply {
                    action = ACTION_COURSE
                    putExtra(CourseAlarmReceiver.EXTRA_COURSE_DATE, event.course.date)
                    putExtra(CourseAlarmReceiver.EXTRA_OCCURRENCE_KEY, CourseReminderDelivery.key(event))
                    putExtra(CourseAlarmReceiver.EXTRA_REMINDER_TRIGGER, event.trigger.toEpochMilliseconds())
                    putExtra(CourseAlarmReceiver.EXTRA_ALARM_SLOT_ID, slot)
                    putExtra(CourseAlarmReceiver.EXTRA_COURSE_ID, event.course.id)
                    putExtra(CourseAlarmReceiver.EXTRA_COURSE_NAME, event.course.name)
                    putExtra(CourseAlarmReceiver.EXTRA_COURSE_POSITION, event.course.position)
                    putExtra(CourseAlarmReceiver.EXTRA_COURSE_TEACHER, event.course.teacher)
                    putExtra(CourseAlarmReceiver.EXTRA_COURSE_START, event.course.startTime)
                    putExtra(CourseAlarmReceiver.EXTRA_COURSE_END, event.course.endTime)
                }
                setAlarm(slot, event.trigger.toEpochMilliseconds(), intent)
            }
            previews.forEachIndexed { index, event ->
                currentCoroutineContext().ensureActive()
                val intent = Intent(applicationContext, SchedulePreviewReceiver::class.java).apply {
                    action = ACTION_PREVIEW
                    putExtra("date", event.date.toString())
                }
                setAlarm(50200 + index, event.trigger.toEpochMilliseconds(), intent)
            }
        } else {
            currentCoroutineContext().ensureActive()
            cancelAllSlots()
        }
        return Result.success()
    }

    private fun cancelAllSlots() {
        cancelSlots(CourseAlarmReceiver::class.java, ACTION_COURSE, 50010, 101)
        cancelSlots(SchedulePreviewReceiver::class.java, ACTION_PREVIEW, 50200, 7)
    }

    private fun setAlarm(slot: Int, at: Long, intent: Intent) {
        val pending = PendingIntent.getBroadcast(applicationContext, slot, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } catch (_: SecurityException) { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending) }
    }

    private fun cancelSlots(receiver: Class<*>, actionName: String, first: Int, count: Int) {
        repeat(count) { index ->
            val intent = Intent(applicationContext, receiver).apply { action = actionName }
            PendingIntent.getBroadcast(applicationContext, first + index, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
                alarms.cancel(it); it.cancel()
            }
        }
    }

    companion object {
        private val schedulingMutex = Mutex()
        const val ACTION_COURSE = "com.xingheyuzhuan.shiguangschedule.ACTION_COURSE_REMIND"
        const val ACTION_PREVIEW = "cn.hbmu.schedule.ACTION_SCHEDULE_PREVIEW"
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork("CourseNotificationWorker_Sync_Update", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<CourseNotificationWorker>().build())
        }
        fun schedulePeriodic(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("hbmu-reminder-refresh", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<CourseNotificationWorker>(12, TimeUnit.HOURS).build())
        }
    }
}
