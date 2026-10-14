package com.xingheyuzhuan.shiguangschedule.ui.schedule

import com.xingheyuzhuan.shiguangschedule.ui.components.*
import com.xingheyuzhuan.shiguangschedule.data.model.*
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto
import kotlinx.datetime.*
import kotlin.test.*

class CourseTemporalStatusTest {
    private val day = LocalDate.parse("2026-10-14")
    private fun status(at: String, date: LocalDate = day, start: String? = "08:00", end: String? = "09:30") =
        courseTemporalStatus(date, start, end, LocalDateTime.parse(at))
    @Test fun classStartIsInclusiveAndEndIsExclusive() {
        assertEquals(CourseTemporalStatus.UPCOMING, status("2026-10-14T07:59:59"))
        assertEquals(CourseTemporalStatus.ONGOING, status("2026-10-14T08:00:00"))
        assertEquals(CourseTemporalStatus.ONGOING, status("2026-10-14T09:29:59"))
        assertEquals(CourseTemporalStatus.COMPLETED, status("2026-10-14T09:30:00"))
    }
    @Test fun matchingWeekdayAndTimeInAnotherWeekCannotBeOngoing() {
        assertEquals(CourseTemporalStatus.COMPLETED, status("2026-10-21T08:15:00"))
        assertEquals(CourseTemporalStatus.UPCOMING, status("2026-10-07T08:15:00"))
    }
    @Test fun coursesMoveToCompletedAcrossMidnight() {
        assertEquals(CourseTemporalStatus.COMPLETED, status("2026-10-15T00:00:00"))
        assertEquals(CourseTemporalStatus.UPCOMING, status("2026-10-13T23:59:59"))
    }
    @Test fun exactAdjustedEndTimeWinsOverFallbackBounds() {
        assertEquals(CourseTemporalStatus.ONGOING, status("2026-10-14T09:35:00", end = "09:40"))
        assertEquals(CourseTemporalStatus.COMPLETED, status("2026-10-14T09:40:00", end = "09:40"))
    }
    @Test fun missingOrInvalidBoundsAreNotClaimedToBeOngoing() {
        assertEquals(CourseTemporalStatus.UNKNOWN, status("2026-10-14T08:15:00", start = null))
        assertEquals(CourseTemporalStatus.UNKNOWN, status("2026-10-14T08:15:00", end = "25:00"))
        assertEquals(CourseTemporalStatus.UNKNOWN, status("2026-10-14T08:15:00", end = "07:00"))
    }
    @Test fun allIndependentPreferencesSurviveWireSerialization() {
        val configured = ScheduleGridStyle.DEFAULT.copy(dimCompletedText = false, dimCompletedBackground = true,
            highlightOngoing = false, strikeCompletedToday = false, hideReminderTip = true)
        val roundTrip = ScheduleGridStyleProto.ADAPTER.decode(ScheduleGridStyleProto.ADAPTER.encode(configured.toProto())).toCompose()
        assertFalse(roundTrip.dimCompletedText)
        assertTrue(roundTrip.dimCompletedBackground)
        assertFalse(roundTrip.highlightOngoing)
        assertFalse(roundTrip.strikeCompletedToday)
        assertTrue(roundTrip.hideReminderTip)
    }
    @Test fun oldPreferencesUseTheNewDefaultsAndRetainWidgetPreferences() {
        val old = ScheduleGridStyleProto(widget_style = WidgetStyle(fontScale = 1.4f, hideTeacher = true).toProto()).toCompose()
        assertTrue(old.dimCompletedText && old.dimCompletedBackground && old.highlightOngoing && old.strikeCompletedToday)
        assertFalse(old.hideReminderTip)
        assertEquals(1.4f, old.widgetStyle.fontScale)
        assertTrue(old.widgetStyle.hideTeacher)
    }
}
