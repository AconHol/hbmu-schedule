package com.xingheyuzhuan.shiguangschedule.hbmu

import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.*

/** Join exact dated course times, never infer the breaks inside a multi-period lesson. */
object HbmuCalendarParser {
    private data class Key(val date: String, val name: String, val periods: List<Int>)
    private data class Times(val start: String, val end: String)

    private fun periods(text: String): List<Int> {
        val clean = text.trim()
        val range = Regex("^(\\d{1,2})\\s*[-—~至]\\s*(\\d{1,2})$").matchEntire(clean)
        val values = when {
            range != null -> (range.groupValues[1].toInt()..range.groupValues[2].toInt()).toList()
            clean.matches(Regex("(?:[0-9]{2})+")) -> clean.chunked(2).map { it.toInt() }
            else -> clean.split(Regex("[,，、\\s]+")).map { it.toIntOrNull() ?: error("学校节次格式异常") }
        }.distinct().sorted()
        require(values.isNotEmpty() && values.all { it in 1..30 }) { "学校节次范围异常" }
        return values
    }

    private fun time(text: String): String {
        val match = Regex("^(\\d{1,2}):(\\d{2})(?::\\d{2})?$").matchEntire(text.trim())
            ?: error("学校上课时间格式异常")
        val formatted = match.groupValues[1].padStart(2, '0') + ":" + match.groupValues[2]
        LocalTime.parse(formatted)
        return formatted
    }

    fun applyTimes(model: CourseImportExport.CourseTableImportModel, responses: List<String>): CourseImportExport.CourseTableImportModel {
        if (model.courses.isEmpty()) return model
        val dates = model.config?.semesterStartDate?.let { LocalDate.parse(it) } ?: error("缺少学期起始日期")
        val times = mutableMapOf<Key, Times>()
        responses.forEach { body ->
            if (body.trimStart().startsWith("<")) throw HbmuSessionExpired()
            val rows = Json.parseToJsonElement(body) as? JsonArray ?: error("学校课程时间返回格式异常，已保留本地课表")
            rows.forEach rowLoop@ { value ->
                val row = value.jsonObject
                fun field(key: String) = (row[key] as? JsonPrimitive)?.contentOrNull.orEmpty().trim()
                if (field("lx") != "kb") return@rowLoop
                // The calendar also labels multi-day activities as kb, with no weekday or periods.
                // Do not let those activities invalidate genuine dated lessons.
                if (field("jcdm").isBlank() && field("xq").isBlank()) return@rowLoop
                val date = LocalDate.parse(field("qsrq").take(10)).toString()
                require(field("jsrq").take(10) == date) { "课程跨日时间异常，已保留本地课表" }
                val key = Key(date, field("kcmc"), periods(field("jcdm")))
                val timing = Times(time(field("qssj")), time(field("jssj")))
                require(timing.start < timing.end) { "课程起止时间异常，已保留本地课表" }
                require(times[key] == null || times[key] == timing) { "学校课程时间存在冲突，已保留本地课表" }
                times[key] = timing
            }
        }
        val courses = model.courses.flatMap { course -> course.weeks.map { week ->
            val date = LocalDate.fromEpochDays(dates.toEpochDays() + (week - 1) * 7L + course.day - 1).toString()
            val sections = (requireNotNull(course.startSection)..requireNotNull(course.endSection)).toList()
            val timing = times[Key(date, course.name, sections)]
                ?: error("学校未完整返回课程时间，已保留本地课表，请稍后重试")
            // Keep section positioning in the weekly grid, with authoritative time overrides.
            course.copy(weeks = listOf(week), customStartTime = timing.start, customEndTime = timing.end)
        } }
        return model.copy(courses = courses)
    }
}
