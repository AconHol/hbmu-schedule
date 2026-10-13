package com.xingheyuzhuan.shiguangschedule.hbmu

import kotlin.test.*

class HbmuScheduleParserTest {
    private fun row(id: String = "1", periods: String = "060708", week: Int = 6, date: String = "2026-10-06", day: Int = 2) =
        """{"dgksdm":"$id","kcmc":"测试课程","teaxms":"教师","zc":"$week","xq":"$day","jcdm":"$periods","jxcdmc":"A101","pkrq":"$date"}"""
    private fun body(vararg rows: String, total: Int = rows.size) = """{"total":$total,"rows":[${rows.joinToString(",") }]}"""

    @Test fun preservesPeriodsRoomAndSemesterStart() {
        val parsed = HbmuScheduleParser.parse(body(row()))
        val course = parsed.model.courses.single()
        assertEquals(6, course.startSection); assertEquals(8, course.endSection)
        assertEquals("A101", course.position); assertEquals(listOf(6), course.weeks)
        assertEquals("2026-08-31", parsed.model.config?.semesterStartDate)
    }
    @Test fun rejectsIncompleteResponse() { assertFailsWith<IllegalArgumentException> { HbmuScheduleParser.parse(body(row(), total = 2)) } }
    @Test fun rejectsDuplicateRecords() { assertFailsWith<IllegalArgumentException> { HbmuScheduleParser.parse(body(row(), row())) } }
    @Test fun recognizesExpiredLoginHtml() { assertFailsWith<HbmuSessionExpired> { HbmuScheduleParser.parse("<html>login</html>") } }
    @Test fun rejectsMalformedJsonEnvelope() { assertFailsWith<IllegalStateException> { HbmuScheduleParser.parse("{}") } }
    @Test fun rejectsWeekdayMismatch() { assertFailsWith<IllegalArgumentException> { HbmuScheduleParser.parse(body(row(day = 3))) } }
    @Test fun rejectsConflictingSemesterStarts() { assertFailsWith<IllegalArgumentException> { HbmuScheduleParser.parse(body(row(), row(id = "2", week = 7))) } }
    @Test fun splitsNonContiguousPeriods() {
        val courses = HbmuScheduleParser.parse(body(row(periods = "010305"))).model.courses
        assertEquals(listOf(1, 3, 5), courses.map { it.startSection })
        assertTrue(courses.all { it.startSection == it.endSection })
    }
    @Test fun keepsWeekendLessons() {
        assertTrue(HbmuScheduleParser.parse(body(row(day = 7, date = "2026-10-11"))).hasWeekends)
    }
    @Test fun permitsAnExplicitlyEmptySemester() {
        val parsed = HbmuScheduleParser.parse(body())
        assertEquals(0, parsed.total); assertTrue(parsed.model.courses.isEmpty())
    }
    @Test fun selectsTheSchoolDefaultInsteadOfFutureOptions() {
        val terms = HbmuTermParser.parse("<select name='xnxqdm'><option value='203701'>Future</option><option value='202601' selected>Current</option></select>")
        assertEquals("202601", terms.first().code); assertTrue(terms.first().selected)
    }
    @Test fun isolatesTheSemesterSelectFromOtherOptionLists() {
        val terms = HbmuTermParser.parse("<select id='other'><option value='123456'>Other</option></select><select id=\"xnxqdm\"><option value=\"202602\" selected=\"selected\">Spring</option></select>")
        assertEquals(listOf("202602"), terms.map { it.code })
    }
}
