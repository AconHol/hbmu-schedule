// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.service

import android.app.NotificationChannel
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.model.AutoControlMode
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import com.xingheyuzhuan.shiguangschedule.notifications.ReminderPlanner
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * 课程闹钟与模式切换广播接收器。
 */
class CourseAlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val appSettingsRepository: AppSettingsRepository by inject()
    private val widgetRepository: WidgetRepository by inject()

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "course_notification_channel"
        const val EXTRA_COURSE_NAME = "course_name"
        const val EXTRA_COURSE_POSITION = "course_position"
        const val EXTRA_COURSE_TEACHER = "extra_course_teacher"
        const val EXTRA_COURSE_ID = "course_id"
        const val EXTRA_COURSE_START = "course_start"
        const val EXTRA_COURSE_END = "course_end"
        const val EXTRA_COURSE_DATE = "course_date"
        const val EXTRA_OCCURRENCE_KEY = "course_occurrence_key"
        const val EXTRA_REMINDER_TRIGGER = "course_reminder_trigger"

        const val EXTRA_DND_ACTION = "extra_dnd_action"
        const val DND_ACTION_START = "dnd_action_start"
        const val DND_ACTION_END = "dnd_action_end"

        const val EXTRA_ALARM_SLOT_ID = "EXTRA_ALARM_SLOT_ID"
        // 覆盖 50001 (DND) 到 50110 (Course Reminders)
        private const val SLOT_START = 50001
        private const val SLOT_END = 50110

        const val ACTION_DISMISS_NOTIFICATION = "com.xingheyuzhuan.shiguangschedule.ACTION_DISMISS_NOTIFICATION"

        private const val NOTIFICATION_ID = 50010
        private const val TAG = "CourseAlarmReceiver"

        fun toggleMode(context: Context, enableMode: Boolean, modeType: AutoControlMode) {
            val audioManager = context.getSystemService<AudioManager>()
            val notificationManager = context.getSystemService<NotificationManager>()
            if (audioManager == null || notificationManager == null) return
            if (!notificationManager.isNotificationPolicyAccessGranted) return
            when (modeType) {
                AutoControlMode.DND -> {
                    notificationManager.setInterruptionFilter(
                        if (enableMode) NotificationManager.INTERRUPTION_FILTER_PRIORITY
                        else NotificationManager.INTERRUPTION_FILTER_ALL
                    )
                }
                AutoControlMode.SILENT -> {
                    audioManager.ringerMode = if (enableMode) AudioManager.RINGER_MODE_SILENT
                    else AudioManager.RINGER_MODE_NORMAL
                }
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let { ctx ->
            if (intent?.action == ACTION_DISMISS_NOTIFICATION) {
                val notifyId = intent.getIntExtra("target_notification_id", -1)
                if (notifyId != -1) {
                    val nm = ctx.getSystemService<NotificationManager>()
                    val tag = intent.getStringExtra("target_notification_tag")
                    if (tag == null) nm?.cancel(notifyId) else nm?.cancel(tag, notifyId)
                }
                return
            }

            val slotId = intent?.getIntExtra(EXTRA_ALARM_SLOT_ID, -1) ?: -1
            val dndAction = intent?.getStringExtra(EXTRA_DND_ACTION)

            if (intent?.data != null || (slotId !in SLOT_START..SLOT_END && dndAction.isNullOrEmpty())) {
                Log.d(TAG, "已拦截非法或旧版闹钟。")
                return
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val appSettings = appSettingsRepository.getAppSettingsOnce()
                    val modeToUse = appSettings.autoControlMode
                    val isCompatMode = appSettings.compatWearableSync

                    if (!dndAction.isNullOrEmpty()) {
                        when (dndAction) {
                            DND_ACTION_START -> toggleMode(ctx, true, modeToUse)
                            DND_ACTION_END -> {
                                toggleMode(ctx, false, modeToUse)
                                DndSchedulerWorker.enqueueWork(ctx)
                            }
                        }
                    } else {
                        if (!appSettings.reminderEnabled) return@launch
                        if (intent?.action != CourseNotificationWorker.ACTION_COURSE || slotId !in 50010..SLOT_END) return@launch
                        val date = intent.getStringExtra(EXTRA_COURSE_DATE)
                        val key = intent.getStringExtra(EXTRA_OCCURRENCE_KEY)
                        if (date.isNullOrBlank() || key.isNullOrBlank() || !intent.hasExtra(EXTRA_REMINDER_TRIGGER)) {
                            // Replace pre-upgrade alarms instead of trusting their undated payloads.
                            CourseNotificationWorker.enqueue(ctx)
                            return@launch
                        }
                        val zone = TimeZone.currentSystemDefault()
                        val now = Clock.System.now()
                        val event = widgetRepository.getWidgetCoursesByDateRange(date, date).first()
                            .mapNotNull { ReminderPlanner.courseReminder(it, zone, appSettings.remindBeforeMinutes) }
                            .firstOrNull { CourseReminderDelivery.key(it) == key } ?: return@launch
                        if (event.trigger.toEpochMilliseconds() != intent.getLongExtra(EXTRA_REMINDER_TRIGGER, Long.MIN_VALUE) ||
                            !ReminderPlanner.canDeliver(event, now, zone)) return@launch
                        val delivered = CourseReminderDelivery.gate(ctx).deliverOnce(key, now.toEpochMilliseconds(),
                            (event.trigger + 2.days).toEpochMilliseconds()) {
                            val course = event.course
                            showNotification(ctx, "course_$key", course.name, course.position, course.teacher,
                                isCompatMode, course.startTime, course.endTime)
                        }
                        if (delivered) updateAllWidgets(ctx)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onReceive 异常", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showNotification(
        context: Context,
        notificationTag: String,
        name: String,
        position: String,
        teacher: String,
        isCompatMode: Boolean,
        startTime: String,
        endTime: String
    ): Boolean {
        val nm = context.getSystemService<NotificationManager>() ?: return false
        if (!nm.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false

        val alertTitle = context.getString(R.string.notification_title_course_alert)
        val closeActionText = context.getString(R.string.action_close)
        val liveStatusText = context.getString(R.string.notification_live_status_preparing)

        val formattedPosition = context.getString(R.string.course_position_prefix, position)
        val formattedTeacher = if (teacher.isNotBlank()) {
            context.getString(R.string.course_teacher_prefix, teacher)
        } else null

        val timeDescription = if (startTime.isBlank()) "" else "$startTime — $endTime\n"
        val bigTextContent = timeDescription + if (formattedTeacher != null) {
            "$formattedPosition\n$formattedTeacher"
        } else {
            formattedPosition
        }

        if (nm.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.item_course_reminder),
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val dismissIntent = Intent(context, CourseAlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_NOTIFICATION
            data = Uri.parse("hbmu-schedule://dismiss/$notificationTag")
            putExtra("target_notification_id", NOTIFICATION_ID)
            putExtra("target_notification_tag", notificationTag)
        }
        val dismissPI = PendingIntent.getBroadcast(
            context, NOTIFICATION_ID, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(name)
            .bigText(bigTextContent)

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .setContentTitle(name)
            .setContentText("${startTime.takeIf { it.isNotBlank() }?.let { "$it · " }.orEmpty()}$position")
            .setSubText(alertTitle)
            .setStyle(bigTextStyle)

            // 非兼容模式应用
            .setOngoing(!isCompatMode)
            .setAutoCancel(isCompatMode)
            .setTimeoutAfter(3 * 60 * 60 * 1000L)

            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setShowWhen(true)
            .addAction(0, closeActionText, dismissPI)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java).putExtra("open_week_schedule", true),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

        if (Build.VERSION.SDK_INT >= 36 && !isCompatMode) {
            builder.setRequestPromotedOngoing(true)
            builder.setShortCriticalText(liveStatusText)
        }
        nm.notify(notificationTag, NOTIFICATION_ID, builder.build())
        return true
    }
}
