// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared geometry for the week controls and the connected timetable cells. */
internal object ConnectedScheduleLayout {
    val PageMargin = 20.dp
    val HalfGap = 2.dp
    val Gap = HalfGap * 2
    val OuterCorner = 28.dp
    val InnerCorner = 6.dp

    fun shape(
        topStart: Boolean = false,
        topEnd: Boolean = false,
        bottomStart: Boolean = false,
        bottomEnd: Boolean = false,
        inner: Dp = InnerCorner,
        outer: Dp = OuterCorner
    ) = RoundedCornerShape(
        topStart = if (topStart) outer else inner,
        topEnd = if (topEnd) outer else inner,
        bottomStart = if (bottomStart) outer else inner,
        bottomEnd = if (bottomEnd) outer else inner
    )
}
