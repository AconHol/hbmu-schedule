package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import kotlin.time.Clock

enum class CourseTemporalStatus { UPCOMING, ONGOING, COMPLETED, UNKNOWN }

/** Exact course bounds override the fallback timetable; end time is exclusive. */
fun courseTemporalStatus(date: LocalDate, start: String?, end: String?, now: LocalDateTime): CourseTemporalStatus {
    if (date < now.date) return CourseTemporalStatus.COMPLETED
    if (date > now.date) return CourseTemporalStatus.UPCOMING
    val begin = start?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
    val finish = end?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
    if (begin == null || finish == null || finish <= begin) return CourseTemporalStatus.UNKNOWN
    return when {
        now.time < begin -> CourseTemporalStatus.UPCOMING
        now.time < finish -> CourseTemporalStatus.ONGOING
        else -> CourseTemporalStatus.COMPLETED
    }
}

/** Only used by the preview harness to exercise real UI at deterministic boundaries. */
val LocalCourseNowOverride = staticCompositionLocalOf<LocalDateTime?> { null }

@Composable
fun rememberCourseNow(): LocalDateTime {
    val override = LocalCourseNowOverride.current
    if (override != null) return override
    val current by produceState(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())) {
        while (true) {
            value = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            delay(1000)
        }
    }
    return current
}
