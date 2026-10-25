// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.notifications

import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import kotlinx.datetime.TimeZone
import kotlin.test.*
import kotlin.time.Instant

class ReminderPlannerTest {
    private val zone = TimeZone.of("Asia/Shanghai")
    private fun course(id: String, date: String = "2026-10-14", start: String = "08:00", skipped: Boolean = false) =
        WidgetCourse(id, "课程", "老师", "教学楼", start, "09:00", skipped, date, 0)

    @Test fun courseRemindersExcludeExpiredSkippedAndInvalidTimes() {
        val now = Instant.parse("2026-10-14T00:00:00Z")
        val result = ReminderPlanner.courses(listOf(course("past"), course("next", start = "10:00"), course("skip", start = "11:00", skipped = true), course("invalid", start = "bad")), now, zone, 15)
        assertEquals(listOf("next"), result.map { it.course.id })
        assertEquals(Instant.parse("2026-10-14T01:45:00Z"), result.single().trigger)
    }

    @Test fun tomorrowPreviewRunsOnPreviousEveningAndGroupsCourses() {
        val result = ReminderPlanner.previews(listOf(course("one"), course("two"), course("skip", date = "2026-10-15", skipped = true)), Instant.parse("2026-10-13T10:00:00Z"), zone, 20)
        assertEquals(1, result.size)
        assertEquals("2026-10-14", result.single().date.toString())
        assertEquals(Instant.parse("2026-10-13T12:00:00Z"), result.single().trigger)
    }

    @Test fun previewsDoNotSendLateOrForInvalidDates() {
        assertTrue(ReminderPlanner.previews(listOf(course("one"), course("bad", date = "bad")), Instant.parse("2026-10-13T12:01:00Z"), zone, 20).isEmpty())
    }

    @Test fun duplicateCoursesCannotCreateDuplicateReminders() {
        val row = course("one")
        assertEquals(1, ReminderPlanner.courses(listOf(row, row), Instant.parse("2026-10-13T00:00:00Z"), zone, 15).size)
    }

    @Test fun reimportedRowsWithDifferentIdsProduceOneReminder() {
        val original = course("old-id")
        val imported = original.copy(id = "new-id", startTime = "08:00:00", endTime = "09:00:00")
        val result = ReminderPlanner.courses(listOf(original, imported), Instant.parse("2026-10-13T00:00:00Z"), zone, 15)
        assertEquals(1, result.size)
        assertEquals(ReminderPlanner.occurrenceKey(original), ReminderPlanner.occurrenceKey(imported))
    }

    @Test fun differentOccurrencesAreNotLostWhenDatabaseIdsMatch() {
        val row = course("same-id")
        val nextDay = row.copy(date = "2026-10-15")
        val nextTime = row.copy(startTime = "10:00", endTime = "11:00")
        assertEquals(3, ReminderPlanner.courses(listOf(row, nextDay, nextTime),
            Instant.parse("2026-10-13T00:00:00Z"), zone, 15).size)
    }

    @Test fun distinctCoursesAtTheSameTimeRemainSeparate() {
        val row = course("one")
        val otherCourse = row.copy(id = "two", name = "另一门课程")
        val otherRoom = row.copy(id = "three", position = "另一间教室")
        assertEquals(3, ReminderPlanner.courses(listOf(row, otherCourse, otherRoom),
            Instant.parse("2026-10-13T00:00:00Z"), zone, 15).size)
    }

    @Test fun deliveryRejectsEarlyAndExpiredAlarmsButAllowsInexactDelay() {
        val event = assertNotNull(ReminderPlanner.courseReminder(course("one"), zone, 15))
        assertFalse(ReminderPlanner.canDeliver(event, Instant.parse("2026-10-13T23:44:59Z"), zone))
        assertTrue(ReminderPlanner.canDeliver(event, Instant.parse("2026-10-13T23:45:00Z"), zone))
        assertTrue(ReminderPlanner.canDeliver(event, Instant.parse("2026-10-14T00:05:00Z"), zone))
        assertFalse(ReminderPlanner.canDeliver(event, Instant.parse("2026-10-14T01:00:00Z"), zone))
        assertFalse(ReminderPlanner.canDeliver(event, Instant.parse("2026-10-15T00:00:00Z"), zone))
    }

    @Test fun midnightLessonCanBeRemindedOnPreviousDate() {
        val row = course("midnight", start = "00:10").copy(endTime = "01:00")
        val event = assertNotNull(ReminderPlanner.courseReminder(row, zone, 15))
        assertTrue(ReminderPlanner.canDeliver(event, Instant.parse("2026-10-13T15:55:00Z"), zone))
    }

    @Test fun cancelledAndMalformedLessonsCannotBeDelivered() {
        val event = assertNotNull(ReminderPlanner.courseReminder(course("one"), zone, 15))
        val now = Instant.parse("2026-10-14T00:05:00Z")
        assertFalse(ReminderPlanner.canDeliver(event.copy(course = event.course.copy(isSkipped = true)), now, zone))
        assertFalse(ReminderPlanner.canDeliver(event.copy(course = event.course.copy(endTime = "bad")), now, zone))
        assertFalse(ReminderPlanner.canDeliver(event.copy(course = event.course.copy(endTime = "07:00")), now, zone))
    }
}
