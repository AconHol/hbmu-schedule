package com.xingheyuzhuan.shiguangschedule.hbmu

import com.xingheyuzhuan.shiguangschedule.data.repository.defaultTimeSlots
import com.xingheyuzhuan.shiguangschedule.data.repository.legacyDefaultTimeSlots
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport
import kotlin.test.*

class HbmuDefaultTimesTest {
    @Test fun baselineCalibrationKeepsExactDatedCourseTimes() {
        val course = CourseImportExport.ImportCourseJsonModel("Demo", "", "", 1, 1, 2, listOf(7),
            customStartTime = "08:00", customEndTime = "09:40")
        val original = CourseImportExport.CourseTableImportModel(listOf(course),
            config = CourseImportExport.CourseConfigJsonModel(semesterStartDate = "2026-08-31"))
        val calibrated = HbmuDefaultTimes.withCalibratedBaseline(original)
        assertEquals(original.courses, calibrated.courses)
        assertEquals("09:40", calibrated.courses.single().customEndTime)
        assertEquals(40, calibrated.config?.defaultClassDuration)
        assertEquals(12, calibrated.timeSlots?.size)
        assertTrue(original.timeSlots.orEmpty().isEmpty())
    }
    @Test fun migratesOnlyUntouchedLegacySlotsAcrossTableIdsAndOrdering() {
        assertTrue(HbmuDefaultTimes.shouldReplaceLegacy(legacyDefaultTimeSlots.reversed().map { it.copy(timeTableId = "demo") }))
        assertFalse(HbmuDefaultTimes.shouldReplaceLegacy(legacyDefaultTimeSlots.dropLast(1)))
        assertFalse(HbmuDefaultTimes.shouldReplaceLegacy(legacyDefaultTimeSlots.map { if (it.number == 1) it.copy(startTime = "07:50") else it }))
        assertFalse(HbmuDefaultTimes.shouldReplaceLegacy(legacyDefaultTimeSlots.map { if (it.number == 1) it.copy(alias = "custom") else it }))
        assertFalse(HbmuDefaultTimes.shouldReplaceLegacy(defaultTimeSlots))
    }
    @Test fun calibratedFallbackAgreesWithObservedCourseBoundaries() {
        assertEquals(12, defaultTimeSlots.size)
        val slots = defaultTimeSlots.associateBy { it.number }
        assertEquals("08:00", slots.getValue(1).startTime)
        assertEquals("09:30", slots.getValue(2).endTime)
        assertEquals("09:40", slots.getValue(3).startTime)
        assertEquals("10:20", slots.getValue(3).endTime)
        assertEquals("10:30", slots.getValue(4).startTime)
        assertEquals("11:10", slots.getValue(4).endTime)
        assertEquals("12:00", slots.getValue(5).endTime)
        assertEquals("14:30", slots.getValue(6).startTime)
        assertEquals("16:00", slots.getValue(7).endTime)
        assertEquals("16:10", slots.getValue(8).startTime)
        assertEquals("17:40", slots.getValue(9).endTime)
        assertEquals("19:00", slots.getValue(10).startTime)
        assertEquals("19:40", slots.getValue(10).endTime)
        assertEquals("20:30", slots.getValue(11).endTime)
        assertEquals("20:40", slots.getValue(12).startTime)
        assertEquals("21:20", slots.getValue(12).endTime)
    }
}
