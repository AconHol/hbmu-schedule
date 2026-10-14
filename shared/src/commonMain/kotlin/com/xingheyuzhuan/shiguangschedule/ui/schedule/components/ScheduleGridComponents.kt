// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.MergedCourseBlock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * 课表项统一调度接口
 */
interface ISingleSchedulable {
    val columnIndex: Int
    val subColumnIndex: Int
    val subColumnCount: Int
    val startSection: Float
    val endSection: Float
    val courseWrapper: CourseWithWeeks
    val parentBlock: MergedCourseBlock
}

/**
 * 核心设计：移动意图（Move Intent）
 */
data class CourseMoveIntent(
    val parentBlock: MergedCourseBlock,     // 原始被拖拽的块
    val initialDay: Int,                   // 拖拽前是周几 (1..7)
    val initialStartSection: Float,        // 拖拽前的起始节次/时间
    val duration: Float                    // 课程跨越的时间/节次长度 (end - start)
)

/**
 * 1. 数据收拢：外部向课表传递的纯展示数据与基础配置
 */
@Immutable
data class ScheduleGridViewState(
    val dates: List<String>,
    val currentYear: String,
    val currentWeek: String? = null,
    val timeSlots: List<TimeSlot>,
    val mergedCourses: List<MergedCourseBlock>,
    val showWeekends: Boolean,
    val todayIndex: Int,
    val firstDayOfWeek: Int,
    val currentSectionIndex: Int = -1,
    val revealEmptyDays: Boolean = false,
    val weekStartDate: kotlinx.datetime.LocalDate? = null
)

/**
 * 2. 动作收拢：外部业务逻辑响应的交互回调接口
 */
interface ScheduleGridActions {
    fun onCourseBlockClicked(block: MergedCourseBlock)
    fun onGridCellClicked(day: Int, section: Int)
    fun onTimeSlotClicked()
    fun onHoldStateChanged(isHolding: Boolean) {}
    fun onCourseMovedWithinGrid(block: MergedCourseBlock, newDay: Int, newStartSection: Float, newEndSection: Float) {}
    fun onCourseTimeAdjusted(block: MergedCourseBlock, newStart: Float, newEnd: Float) {}
    fun onInitiateFloatingMode(block: MergedCourseBlock) {}
}

/**
 * 3. 运行状态收拢：课表内部手势运行时的临时状态管理类
 */
@Stable
class ScheduleGridState(
    val gridScrollState: ScrollState
) {
    var expandedItem by mutableStateOf<ISingleSchedulable?>(null)

    var activeMoveIntent by mutableStateOf<CourseMoveIntent?>(null)
    var bodyDragOffsetX by mutableStateOf(0f)
    var bodyDragOffsetY by mutableStateOf(0f)

    var isTopHandleDragging by mutableStateOf(false)
    var isBottomHandleDragging by mutableStateOf(false)

    var topHandleDragOffsetY by mutableStateOf(0f)
    var bottomHandleDragOffsetY by mutableStateOf(0f)

    var gridWidthPx by mutableStateOf(0f)
    var viewportHeightPx by mutableStateOf(0f)

    val isEditingActive: Boolean
        get() = expandedItem != null && (activeMoveIntent != null || isTopHandleDragging || isBottomHandleDragging)

    fun resetAllStates() {
        expandedItem = null
        activeMoveIntent = null
        isTopHandleDragging = false
        isBottomHandleDragging = false
        topHandleDragOffsetY = 0f
        bottomHandleDragOffsetY = 0f
        bodyDragOffsetX = 0f
        bodyDragOffsetY = 0f
    }
}

/**
 * 方便在 Composable 中创建并记住 ScheduleGridState 实例的快捷函数
 */
@Composable
fun rememberScheduleGridState(
    gridScrollState: ScrollState = rememberScrollState()
): ScheduleGridState {
    return remember(gridScrollState) {
        ScheduleGridState(gridScrollState)
    }
}

/**
 * 课表顶部日期/星期标头组件
 */
@Composable
fun DayHeader(
    style: ScheduleGridStyleComposed,
    displayDays: List<String>, dates: List<String>, currentYear: String,
    todayIndex: Int, textColor: Color, subTextColor: Color
) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().height(style.dayHeaderHeight.coerceAtLeast(60.dp)),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(style.timeColumnWidth).fillMaxHeight()
            .padding(end = ConnectedScheduleLayout.HalfGap, bottom = ConnectedScheduleLayout.HalfGap)
            .clip(ConnectedScheduleLayout.shape(topStart = true)).background(colors.surfaceContainerHighest), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
            Text(currentYear, style = MaterialTheme.typography.labelSmall, color = subTextColor)
            Text("节次", style = MaterialTheme.typography.labelSmall, color = subTextColor)
        }
        displayDays.forEachIndexed { index, day ->
            val selected = index == todayIndex
            Box(Modifier.weight(1f).semantics { contentDescription = "课表日期：$day ${dates.getOrNull(index).orEmpty()}" }.padding(start = ConnectedScheduleLayout.HalfGap,
                end = if (index == displayDays.lastIndex) 0.dp else ConnectedScheduleLayout.HalfGap,
                bottom = ConnectedScheduleLayout.HalfGap), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Surface(
                    shape = ConnectedScheduleLayout.shape(topEnd = index == displayDays.lastIndex),
                    color = if (selected) colors.primary else colors.secondaryContainer,
                    contentColor = if (selected) colors.onPrimary else textColor,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                        Text(day, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        if (!style.hideDateUnderDay) Text(dates.getOrNull(index).orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) colors.onPrimary.copy(alpha = .82f) else subTextColor)
                    }
                }
            }
        }
    }
}

@Composable
fun TimeColumn(
    style: ScheduleGridStyleComposed,
    timeSlots: List<TimeSlot>,
    maxGridSections: Int,
    is24HourMode: Boolean,
    onTimeSlotClicked: () -> Unit,
    modifier: Modifier,
    currentSectionIndex: Int = -1,
    textColor: Color,
    subTextColor: Color,
    activeDragHour: Int? = null,
    activeDragMinuteStr: String? = null,
    showClockTimes: Boolean = true
) {
    val currentHour = remember {
        try {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
        } catch (e: Exception) {
            -1
        }
    }

    Column(modifier.width(style.timeColumnWidth)) {
        for (index in 0 until maxGridSections) {
            val isCurrentHourActive = if (is24HourMode) {
                currentHour == index && currentSectionIndex != -1
            } else {
                index + 1 == currentSectionIndex
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(style.sectionHeight)
                    .padding(top = ConnectedScheduleLayout.HalfGap, end = ConnectedScheduleLayout.HalfGap,
                        bottom = if (index == maxGridSections - 1) 0.dp else ConnectedScheduleLayout.HalfGap)
                    .clip(ConnectedScheduleLayout.shape(bottomStart = index == maxGridSections - 1))
                    .background(if (isCurrentHourActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable { onTimeSlotClicked() },
                contentAlignment = if (is24HourMode) Alignment.TopCenter else Alignment.Center
            ) {
                val h = maxHeight
                if (is24HourMode && activeDragMinuteStr != null && index == activeDragHour) {
                    Box(
                        modifier = Modifier.matchParentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ":$activeDragMinuteStr",
                            fontSize = if (h < 32.dp) 11.sp else 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (is24HourMode) Arrangement.Top else Arrangement.Center,
                    modifier = Modifier
                        .padding(horizontal = 1.dp)
                        .then(if (is24HourMode) Modifier.padding(top = 4.dp) else Modifier)
                ) {
                    if (is24HourMode) {
                        val formatHourStr = "${index.toString().padStart(2, '0')}:00"
                        Text(
                            text = formatHourStr,
                            fontSize = if (h < 32.dp) 11.sp else 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isCurrentHourActive) MaterialTheme.colorScheme.primary else textColor
                        )
                    } else {
                        val slot = timeSlots.getOrNull(index)
                        if (slot != null) {
                            Text(
                                text = slot.alias ?: slot.number.toString(),
                                fontSize = if (h < 32.dp) 11.sp else 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!style.hideSectionTime && showClockTimes) {
                                when {
                                    h >= 52.dp -> {
                                        Spacer(Modifier.height(2.dp))
                                        TimeText(slot.startTime, subTextColor)
                                        TimeText(slot.endTime, subTextColor)
                                    }
                                    h >= 38.dp -> {
                                        Text(text = "${slot.startTime}-${slot.endTime}", fontSize = 11.sp, color = subTextColor, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 课表编辑状态下的拉伸手柄（上下边界调整）
 */
@Composable
fun BoxScope.CourseEditHandles(
    onDragStart: (isTop: Boolean) -> Unit,
    onDragging: (deltaY: Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val handleBoxSize = 32.dp

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = (-16).dp, y = (-16).dp)
            .size(handleBoxSize)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDragStart(true) },
                    onDragEnd = { onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragging(dragAmount.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        EditHandleDot()
    }

    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(x = 16.dp, y = 16.dp)
            .size(handleBoxSize)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDragStart(false) },
                    onDragEnd = { onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragging(dragAmount.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        EditHandleDot()
    }
}

@Composable
private fun EditHandleDot() {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
    )
}

@Composable
fun TimeText(text: String, color: Color) {
    Text(text = text, fontSize = 10.sp, color = color, style = TextStyle(lineHeight = 1.em))
}

/**
 * 计算扁平化的可调度项列表
 */
fun calculateSingleSchedulables(
    mergedCourses: List<MergedCourseBlock>,
    firstDayOfWeek: Int,
    showWeekends: Boolean,
    displayDays: List<Int> = (0 until if (showWeekends) 7 else 5).map { mapDisplayIndexToDay(it, firstDayOfWeek) }
): List<ISingleSchedulable> {
    val list = mutableListOf<ISingleSchedulable>()
    mergedCourses.forEach { block ->
        val displayIdx = displayDays.indexOf(block.day)
        if (displayIdx != -1) {
            val layoutInfo = block.nonActiveRanges.firstOrNull() ?: (0f to 1f)
            block.courses.firstOrNull()?.let { course ->
                list.add(object : ISingleSchedulable {
                    override val columnIndex = displayIdx
                    override val subColumnIndex = layoutInfo.first.toInt()
                    override val subColumnCount = layoutInfo.second.toInt()
                    override val startSection = block.startSection
                    override val endSection = block.endSection
                    override val courseWrapper = course
                    override val parentBlock = block.copy(courses = listOf(course))
                })
            }
        }
    }
    return list
}

fun rearrangeDays(originalDays: List<String>, firstDayOfWeek: Int): List<String> {
    val startIndex = (firstDayOfWeek - 1).coerceIn(0, 6)
    return originalDays.subList(startIndex, originalDays.size) + originalDays.subList(0, startIndex)
}

fun mapDayToDisplayIndex(courseDay: Int, firstDayOfWeek: Int, showWeekends: Boolean): Int {
    val idx = (courseDay - firstDayOfWeek + 7) % 7
    return if (idx >= if (showWeekends) 7 else 5) -1 else idx
}

fun mapDisplayIndexToDay(idx: Int, firstDayOfWeek: Int): Int = (firstDayOfWeek - 1 + idx) % 7 + 1
