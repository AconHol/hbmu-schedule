// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
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

/** Keep the fixed-header junction rounded even when scrolling cuts through a cell. */
internal fun Modifier.clipScheduleViewport(timeColumnWidth: Dp, dayCount: Int) = drawWithCache {
    val gutter = ConnectedScheduleLayout.HalfGap.toPx()
    val radius = ConnectedScheduleLayout.InnerCorner.toPx()
    val axisWidth = timeColumnWidth.toPx()
    val dayWidth = (size.width - axisWidth) / dayCount
    val mask = Path().apply {
        // Below the junction the complete canvas remains available for course dragging.
        addRect(Rect(0f, gutter + radius, size.width, size.height))
        fun topCell(left: Float, right: Float) {
            addRoundRect(RoundRect(left, gutter, right, size.height,
                topLeftCornerRadius = CornerRadius(radius), topRightCornerRadius = CornerRadius(radius)))
        }
        topCell(0f, axisWidth - gutter)
        repeat(dayCount) { day ->
            topCell(axisWidth + day * dayWidth + gutter,
                axisWidth + (day + 1) * dayWidth - if (day == dayCount - 1) 0f else gutter)
        }
    }
    onDrawWithContent { clipPath(mask) { this@onDrawWithContent.drawContent() } }
}
