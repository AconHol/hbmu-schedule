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
}
