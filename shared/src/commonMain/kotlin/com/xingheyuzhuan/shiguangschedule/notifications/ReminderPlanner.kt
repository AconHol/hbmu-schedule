// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.notifications

import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

data class CourseReminder(val course: WidgetCourse, val trigger: Instant)
data class SchedulePreview(val date: LocalDate, val trigger: Instant)

object ReminderPlanner {
    fun courses(courses: List<WidgetCourse>, now: Instant, zone: TimeZone, minutesBefore: Int): List<CourseReminder> = courses
        .filterNot { it.isSkipped }.distinctBy { it.id }
        .mapNotNull { course -> runCatching {
            val start = LocalDateTime(LocalDate.parse(course.date), LocalTime.parse(course.startTime)).toInstant(zone)
            CourseReminder(course, start - minutesBefore.coerceIn(0, 120).minutes)
        }.getOrNull() }
        .filter { it.trigger > now }.sortedBy { it.trigger }

    fun previews(courses: List<WidgetCourse>, now: Instant, zone: TimeZone, hour: Int): List<SchedulePreview> = courses
        .filterNot { it.isSkipped }.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.distinct()
        .map { date -> SchedulePreview(date, LocalDateTime(date.minus(1, DateTimeUnit.DAY), LocalTime(hour.coerceIn(0, 23), 0)).toInstant(zone)) }
        .filter { it.trigger > now }.sortedBy { it.trigger }
}
