package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Large outer corners and compact joins, matching connected Material lists. */
fun connectedCourseShape(index: Int, count: Int): RoundedCornerShape = RoundedCornerShape(
    topStart = if (index == 0) 28.dp else 8.dp,
    topEnd = if (index == 0) 28.dp else 8.dp,
    bottomStart = if (index == count - 1) 28.dp else 8.dp,
    bottomEnd = if (index == count - 1) 28.dp else 8.dp
)
