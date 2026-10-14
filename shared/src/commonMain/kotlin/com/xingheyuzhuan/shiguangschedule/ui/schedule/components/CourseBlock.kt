// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTemporalStatus
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.BorderTypeProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import com.xingheyuzhuan.shiguangschedule.ui.theme.LocalIsDarkTheme

@Composable
fun CourseBlock(
    courseWrapper: CourseWithWeeks,
    isVisualDemoted: Boolean,
    style: ScheduleGridStyleComposed,
    timeSlots: List<TimeSlot>,
    modifier: Modifier = Modifier,
    isFloating: Boolean = false, // 标记当前块是否处于长按选中/悬浮状态
    blockShape: Shape? = null,
    temporalStatus: CourseTemporalStatus? = null
) {
    val course = courseWrapper.course
    val isDarkTheme = LocalIsDarkTheme.current

    // 颜色适配
    val colorIndex = course.colorInt.takeIf { it in style.courseColorMaps.indices }
    val courseColorAdapted: Color? = colorIndex?.let { index ->
        val baseColorMap = style.courseColorMaps[index]
        if (isDarkTheme) baseColorMap.dark else baseColorMap.light
    }
    val fallbackColorAdapted: Color = style.courseColorMaps.firstOrNull()?.let { if (isDarkTheme) it.dark else it.light }
        ?: MaterialTheme.colorScheme.secondaryContainer

    val currentAlpha = if (isFloating) 0.95f else style.courseBlockAlpha
    val accent = courseColorAdapted ?: fallbackColorAdapted
    val completed = temporalStatus == CourseTemporalStatus.COMPLETED
    val ongoing = temporalStatus == CourseTemporalStatus.ONGOING && style.highlightOngoing
    val colors = MaterialTheme.colorScheme
    val blockColor = when {
        ongoing -> colors.primary
        completed && style.dimCompletedBackground -> colors.surfaceContainerHighest
        else -> accent
    }.copy(alpha = currentAlpha)
    val textColor = when {
        ongoing -> colors.onPrimary
        completed && style.dimCompletedText -> colors.onSurfaceVariant
        else -> style.courseTextColor ?: colors.onSurface
    }
    val captionColor = if (ongoing) colors.onPrimary.copy(alpha = .82f) else if (completed && style.dimCompletedText) colors.onSurfaceVariant else style.courseTextColor ?: colors.onSurfaceVariant

    // 字体大小
    val s13 = (12 * style.fontScale).sp
    val s10 = (11 * style.fontScale).sp

    // 核心分支逻辑：判断 24小时模式 与 节次模式 的时间文本渲染
    val customStartTime = course.customStartTime
    val customEndTime = course.customEndTime
    val customTimeString = if (customStartTime != null && customEndTime != null) "$customStartTime - $customEndTime" else null
    val isCustomTimeCourse = customTimeString != null

    val timeTextToShow = if (style.scheduleMode == ScheduleModeProto.TIME_24H_MODE) {
        // 24小时绝对时间轴模式：全部课程都显示起止时间
        if (isCustomTimeCourse) {
            customTimeString
        } else {
            val startSlot = timeSlots.find { it.number == course.startSection }
            val endSlot = timeSlots.find { it.number == course.endSection }
            if (startSlot != null && endSlot != null) "${startSlot.startTime} - ${endSlot.endTime}" else null
        }
    } else {
        // 传统节次模式：只有自定义课程显示起止时间；普通节次课程只有在开启展示开始时间时才显示开始时间
        if (isCustomTimeCourse) {
            customTimeString
        } else if (style.showStartTime) {
            timeSlots.find { it.number == course.startSection }?.startTime
        } else {
            null
        }
    }

    // 边框样式配置
    val borderColor = if (isFloating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val borderWidth = if (isFloating) 2.dp else 1.dp
    val borderAlpha = if (isFloating) 1.0f else style.courseBlockAlpha
    val shape = blockShape ?: RoundedCornerShape(style.courseBlockCornerRadius)

    val borderModifier = when (style.borderType) {
        BorderTypeProto.BORDER_TYPE_SOLID -> {
            Modifier.border(borderWidth, borderColor.copy(alpha = borderAlpha), shape)
        }
        BorderTypeProto.BORDER_TYPE_DASHED -> {
            Modifier.drawBehind {
                val strokeWidth = borderWidth.toPx()
                val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                drawOutline(
                    outline = shape.createOutline(size, layoutDirection, this),
                    color = borderColor.copy(alpha = borderAlpha),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, pathEffect = dashPathEffect)
                )
            }
        }
        else -> {
            if (isFloating) Modifier.border(borderWidth, borderColor, shape) else Modifier
        }
    }

    val horizontalAlignment = if (style.textAlignCenterHorizontal) Alignment.CenterHorizontally else Alignment.Start
    val verticalArrangement = Arrangement.spacedBy(4.dp, if (style.textAlignCenterVertical) Alignment.CenterVertically else Alignment.Top)
    val textAlign = if (style.textAlignCenterHorizontal) TextAlign.Center else TextAlign.Start

    // 选中捏起时，增加三维物理阴影
    val floatingShadowModifier = if (isFloating) {
        Modifier.shadow(elevation = 8.dp, shape = shape, clip = false)
    } else {
        Modifier
    }

    BoxWithConstraints(
        modifier = modifier.semantics {
            when (temporalStatus) {
                CourseTemporalStatus.ONGOING -> stateDescription = "正在上课"
                CourseTemporalStatus.COMPLETED -> stateDescription = "已结束"
                else -> Unit
            }
        }
            .then(floatingShadowModifier)
            .fillMaxSize()
            .then(borderModifier)
            .clip(shape)
            .background(color = blockColor)
    ) {
        val blockHeight = maxHeight
        // 课程文字内容容器
        Column(
            modifier = Modifier.fillMaxSize().padding(style.courseBlockInnerPadding),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement
        ) {
            if (timeTextToShow != null) {
                Text(
                    text = timeTextToShow,
                    fontSize = s10,
                    color = captionColor,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = textAlign,
                    style = TextStyle(lineHeight = 1.em)
                )
            }

            Text(
                text = course.name,
                fontSize = s13,
                fontWeight = FontWeight.Bold,
                color = textColor,
                overflow = TextOverflow.Ellipsis,
                maxLines = if (blockHeight >= 110.dp) 4 else 2,
                textAlign = textAlign,
                modifier = Modifier.weight(1f, fill = false),
                style = TextStyle(lineHeight = 1.2.em)
            )

            if (!style.hideLocation && course.position.isNotBlank() && blockHeight >= 60.dp) {
                Text(if (style.removeLocationAt) course.position else "@${course.position}", fontSize = s10, color = captionColor,
                    maxLines = if (blockHeight >= 110.dp) 2 else 1, overflow = TextOverflow.Ellipsis, textAlign = textAlign,
                    style = TextStyle(lineHeight = 1.25.em))
            }
            if (!style.hideTeacher && course.teacher.isNotBlank() && blockHeight >= 112.dp) {
                Text(course.teacher, fontSize = s10, color = captionColor,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = textAlign,
                    style = TextStyle(lineHeight = 1.25.em))
            }
        }

        // 当单课不是当前周时，进行干净的全局遮罩染色与虚化斜线绘制
        if (isVisualDemoted && !isFloating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = (if (isDarkTheme) Color.Black else Color.White).copy(alpha = 0.618f))
                    .drawBehind {
                        val stripeWidth = 5.dp.toPx()
                        val stripeColor = (if (isDarkTheme) Color.White else Color.Black).copy(alpha = 0.06f)
                        val brush = Brush.linearGradient(
                            0.0f to stripeColor, 0.45f to stripeColor,
                            0.55f to Color.Transparent, 1.0f to Color.Transparent,
                            start = Offset(0f, 0f), end = Offset(stripeWidth, stripeWidth), tileMode = TileMode.Repeated
                        )
                        drawRect(brush = brush)
                    }
            )
        }
    }
}
