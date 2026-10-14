package com.xingheyuzhuan.shiguangschedule.ui.schedule

import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.scheduleDisplayedDays
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.model.toCompose
import com.xingheyuzhuan.shiguangschedule.data.model.toProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto
import kotlin.test.*

class ScheduleDayColumnsTest {
    @Test fun nonconsecutiveColumnsKeepTheirRealWeekdayNumbers() {
        assertEquals(listOf(1, 3, 5), scheduleDisplayedDays(1, true, true, setOf(5, 1, 3)))
    }
    @Test fun sundayFirstOrderingIsPreserved() {
        assertEquals(listOf(7, 1, 5), scheduleDisplayedDays(7, true, true, setOf(1, 5, 7)))
    }
    @Test fun weekendVisibilityStillApplies() {
        assertEquals(listOf(1), scheduleDisplayedDays(1, false, true, setOf(1, 6, 7)))
    }
    @Test fun emptyWeekCanExposeAnEmptyState() {
        assertTrue(scheduleDisplayedDays(1, true, true, emptySet()).isEmpty())
    }
    @Test fun turningTheOptionOffRestoresTheConfiguredWeek() {
        assertEquals(listOf(1, 2, 3, 4, 5), scheduleDisplayedDays(1, false, false, setOf(3)))
    }
    @Test fun editingCanRevealEmptyDropTargets() {
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), scheduleDisplayedDays(1, true, true, setOf(3), revealEmptyDays = true))
    }
    @Test fun existingStyleDefaultsToAutomaticHiding() {
        assertTrue(ScheduleGridStyleProto().toCompose().hideEmptyDays)
    }
    @Test fun explicitDisabledPreferenceSurvivesSerialization() {
        val bytes = ScheduleGridStyleProto.ADAPTER.encode(ScheduleGridStyle.DEFAULT.copy(hideEmptyDays = false).toProto())
        assertFalse(ScheduleGridStyleProto.ADAPTER.decode(bytes).toCompose().hideEmptyDays)
    }
}
