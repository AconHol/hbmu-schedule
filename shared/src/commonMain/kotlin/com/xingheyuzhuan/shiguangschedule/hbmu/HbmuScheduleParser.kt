package com.xingheyuzhuan.shiguangschedule.hbmu

import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.serialization.json.*

/** Only complete, unique, structurally valid responses may replace the offline timetable. */
object HbmuScheduleParser {
    data class Parsed(
        val total: Int,
        val model: CourseImportExport.CourseTableImportModel,
        val hasWeekends: Boolean
    )

    fun parse(body: String): Parsed {
        if (body.trimStart().startsWith("<")) throw HbmuSessionExpired()
        val root = runCatching { Json.parseToJsonElement(body).jsonObject }
            .getOrElse { error("教务返回格式异常，已保留本地课表") }
        val total = root["total"]?.jsonPrimitive?.intOrNull
            ?: error("缺少课表总条数，已保留本地课表")
        require(total in 0..10000) { "课表总条数异常" }
        val rows = root["rows"] as? JsonArray ?: error("缺少课表明细，已保留本地课表")
        require(rows.size == total) { "课表不完整：应有 $total 条，实际 ${rows.size} 条，已保留本地课表" }
        val ids = mutableSetOf<String>()
        val starts = mutableSetOf<LocalDate>()
        val courses = mutableListOf<CourseImportExport.ImportCourseJsonModel>()
        rows.forEach { value ->
            val row = value.jsonObject
            fun field(key: String) = (row[key] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
            val id = field("dgksdm")
            require(id.isNotEmpty() && ids.add(id)) { "排课记录缺失或重复，已保留本地课表" }
            val name = field("kcmc")
            val day = field("xq").toIntOrNull() ?: error("星期字段异常")
            val week = field("zc").toIntOrNull() ?: error("周次字段异常")
            require(name.isNotEmpty() && day in 1..7 && week in 1..60) { "课程名称、星期或周次异常" }
            val periodText = field("jcdm")
            require(periodText.matches(Regex("(?:[0-9]{2})+"))) { "节次格式异常：$name" }
            val periods = periodText.chunked(2).map { it.toInt() }.distinct().sorted()
            require(periods.all { it in 1..30 }) { "节次范围异常：$name" }
            val date = LocalDate.parse(field("pkrq"))
            require(date.dayOfWeek.isoDayNumber == day) { "排课日期与星期不一致：$name" }
            starts += LocalDate.fromEpochDays(date.toEpochDays() - (week - 1) * 7L - (day - 1))
            // Split non-contiguous periods instead of introducing lessons in the intervening gap.
            val groups = mutableListOf<MutableList<Int>>()
            periods.forEach { period ->
                if (groups.isEmpty() || groups.last().last() + 1 != period) groups += mutableListOf(period)
                else groups.last() += period
            }
            groups.forEach { sections ->
                courses += CourseImportExport.ImportCourseJsonModel(
                    name = name, teacher = field("teaxms"), position = field("jxcdmc"),
                    day = day, startSection = sections.first(), endSection = sections.last(), weeks = listOf(week),
                    color = (name.hashCode().toLong().let { if (it < 0) -it else it } % 12).toInt(),
                    remark = listOf(field("jxhjmc"), field("sknrjj"), field("bzsm"))
                        .filter { it.isNotBlank() }.joinToString(" · ").take(300).ifBlank { null }
                )
            }
        }
        require(starts.size <= 1) { "教务周次基准不一致，已保留本地课表" }
        return Parsed(total, CourseImportExport.CourseTableImportModel(
            courses = courses,
            config = CourseImportExport.CourseConfigJsonModel(
                semesterStartDate = starts.firstOrNull()?.toString(),
                semesterTotalWeeks = maxOf(20, courses.flatMap { it.weeks }.maxOrNull() ?: 20)
            )
        ), courses.any { it.day >= 6 })
    }
}
