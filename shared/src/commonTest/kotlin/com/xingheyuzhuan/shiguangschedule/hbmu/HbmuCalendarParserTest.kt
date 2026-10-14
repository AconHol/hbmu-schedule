package com.xingheyuzhuan.shiguangschedule.hbmu

import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport
import kotlin.test.*

class HbmuCalendarParserTest {
    private fun model(weeks: List<Int> = listOf(6)) = CourseImportExport.CourseTableImportModel(
        courses = listOf(CourseImportExport.ImportCourseJsonModel("Demo", "", "A101", 4, 1, 2, weeks)),
        config = CourseImportExport.CourseConfigJsonModel(semesterStartDate = "2026-08-31")
    )
    private fun event(date: String = "2026-10-08", start: String = "8:00:00", end: String = "9:40:00", periods: String = "0102", type: String = "kb") =
        """{"lx":"$type","kcmc":"Demo","qsrq":"$date","jsrq":"$date","qssj":"$start","jssj":"$end","jcdm":"$periods"}"""
    @Test fun authoritativeTimesPreserveWeeklySectionPositions() {
        val course = HbmuCalendarParser.applyTimes(model(), listOf("[${event()}]")).courses.single()
        assertEquals("08:00", course.customStartTime)
        assertEquals("09:40", course.customEndTime)
        assertEquals(1, course.startSection)
        assertEquals(2, course.endSection)
        assertFalse(course.isCustomTime)
    }
    @Test fun separateWeeksCanHaveDifferentSeasonalTimes() {
        val courses = HbmuCalendarParser.applyTimes(model(listOf(6, 7)), listOf("[${event()},${event("2026-10-15", "8:30", "10:10")}]" )).courses
        assertEquals(listOf("08:00", "08:30"), courses.map { it.customStartTime })
        assertEquals(listOf(listOf(6), listOf(7)), courses.map { it.weeks })
    }
    @Test fun acceptsDisplayedPeriodRangesAndLists() {
        listOf("1-2", "01,02", "1,2", "0102").forEach { periods ->
            assertEquals("08:00", HbmuCalendarParser.applyTimes(model(), listOf("[${event(periods = periods)}]")).courses.single().customStartTime)
        }
    }
    @Test fun missingTimesDoNotReplaceTheInputModel() {
        val original = model()
        assertFailsWith<IllegalStateException> { HbmuCalendarParser.applyTimes(original, listOf("[]")) }
        assertNull(original.courses.single().customStartTime)
    }
    @Test fun examsCannotSupplyCourseTimes() {
        assertFailsWith<IllegalStateException> { HbmuCalendarParser.applyTimes(model(), listOf("[${event(type = "ks")}]")) }
    }
    @Test fun rejectsConflictingTimesRatherThanPickingTheFirst() {
        assertFailsWith<IllegalArgumentException> { HbmuCalendarParser.applyTimes(model(), listOf("[${event()},${event(start = "10:00", end = "11:40")}]")) }
    }
    @Test fun equalOverlappingCalendarResponsesAreSafe() {
        assertEquals(1, HbmuCalendarParser.applyTimes(model(), listOf("[${event()}]", "[${event()}]")).courses.size)
    }
    @Test fun rejectsInvalidClockTimes() {
        assertFails { HbmuCalendarParser.applyTimes(model(), listOf("[${event(start = "25:00")}]")) }
        assertFails { HbmuCalendarParser.applyTimes(model(), listOf("[${event(start = "10:00", end = "09:00")}]")) }
    }
    @Test fun detectsExpiredCalendarSession() {
        assertFailsWith<HbmuSessionExpired> { HbmuCalendarParser.applyTimes(model(), listOf("<html>login</html>")) }
    }
}
