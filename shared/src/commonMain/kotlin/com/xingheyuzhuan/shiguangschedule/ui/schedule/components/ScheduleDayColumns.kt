package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

/** Keep real weekday numbers when displayed columns are no longer consecutive. */
fun scheduleDisplayedDays(
    firstDayOfWeek: Int,
    showWeekends: Boolean,
    hideEmptyDays: Boolean,
    courseDays: Set<Int>,
    revealEmptyDays: Boolean = false
): List<Int> {
    val days = (0 until if (showWeekends) 7 else 5).map { mapDisplayIndexToDay(it, firstDayOfWeek) }
    return if (hideEmptyDays && !revealEmptyDays) days.filter { it in courseDays } else days
}
