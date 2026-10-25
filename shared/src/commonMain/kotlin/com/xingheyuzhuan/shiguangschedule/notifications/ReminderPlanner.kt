// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.notifications

import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

data class CourseReminder(val course: WidgetCourse, val trigger: Instant) {
    val occurrenceKey: String get() = ReminderPlanner.occurrenceKey(course)
}
data class SchedulePreview(val date: LocalDate, val trigger: Instant)

object ReminderPlanner {
    fun courses(courses: List<WidgetCourse>, now: Instant, zone: TimeZone, minutesBefore: Int): List<CourseReminder> = courses
        .mapNotNull { courseReminder(it, zone, minutesBefore) }
        // Database IDs can change on import; duplicate rows must still describe one lesson.
        .distinctBy { it.occurrenceKey }
        .filter { it.trigger > now }.sortedBy { it.trigger }

    fun courseReminder(course: WidgetCourse, zone: TimeZone, minutesBefore: Int): CourseReminder? {
        if (course.isSkipped) return null
        return runCatching {
            val start = LocalDateTime(LocalDate.parse(course.date), LocalTime.parse(course.startTime)).toInstant(zone)
            CourseReminder(course, start - minutesBefore.coerceIn(0, 120).minutes)
        }.getOrNull()
    }

    fun occurrenceKey(course: WidgetCourse): String {
        fun time(value: String) = runCatching { LocalTime.parse(value.trim()).toString() }.getOrDefault(value.trim())
        return listOf(course.date.trim(), time(course.startTime), time(course.endTime),
            course.name.trim(), course.teacher.trim(), course.position.trim())
            .joinToString("") { "${it.length}:$it" }
    }

    /** Accept delayed alarms while the lesson is relevant, never early or after it ends. */
    fun canDeliver(event: CourseReminder, now: Instant, zone: TimeZone): Boolean = runCatching {
        val date = LocalDate.parse(event.course.date)
        val start = LocalDateTime(date, LocalTime.parse(event.course.startTime)).toInstant(zone)
        val end = LocalDateTime(date, LocalTime.parse(event.course.endTime)).toInstant(zone)
        !event.course.isSkipped && end > start && now >= event.trigger && now < end
    }.getOrDefault(false)

    fun previews(courses: List<WidgetCourse>, now: Instant, zone: TimeZone, hour: Int): List<SchedulePreview> = courses
        .filterNot { it.isSkipped }.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.distinct()
        .map { date -> SchedulePreview(date, LocalDateTime(date.minus(1, DateTimeUnit.DAY), LocalTime(hour.coerceIn(0, 23), 0)).toInstant(zone)) }
        .filter { it.trigger > now }.sortedBy { it.trigger }
}
