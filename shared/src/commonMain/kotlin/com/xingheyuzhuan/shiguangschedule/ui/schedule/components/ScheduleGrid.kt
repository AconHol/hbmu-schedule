// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import kotlinx.coroutines.delay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import com.xingheyuzhuan.shiguangschedule.ui.components.courseTemporalStatus
import com.xingheyuzhuan.shiguangschedule.ui.components.rememberCourseNow
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.week_days_short_names
import shiguangschedule.shared.generated.resources.calendar_today_24px
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun ScheduleGrid(
    state: ScheduleGridState,
    viewState: ScheduleGridViewState,
    actions: ScheduleGridActions,
    style: ScheduleGridStyleComposed,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize().padding(horizontal = ConnectedScheduleLayout.PageMargin)) {
        val density = LocalDensity.current
        val courseNow = rememberCourseNow()
        val horizontalScroll = rememberScrollState()
        var showFullWeek by remember(viewState.dates, style.hideEmptyDays) { mutableStateOf(false) }
        val navigationBottom = com.xingheyuzhuan.shiguangschedule.ui.components.LocalNavigationContentBottom.current

        LaunchedEffect(viewState.mergedCourses) {
            state.resetAllStates()
            actions.onHoldStateChanged(false)
        }

        val pageTextColor = style.pageTextColor ?: MaterialTheme.colorScheme.onSurface
        val pageSubTextColor = pageTextColor.copy(alpha = 0.7f)
        val weekDays = stringArrayResource(Res.array.week_days_short_names).toList()
        val reorderedWeekDays = rearrangeDays(weekDays, viewState.firstDayOfWeek)
        val dayNumbers = scheduleDisplayedDays(viewState.firstDayOfWeek, viewState.showWeekends, style.hideEmptyDays,
            viewState.mergedCourses.map { it.day }.toSet(),
            revealEmptyDays = showFullWeek || viewState.revealEmptyDays || state.expandedItem != null)
        val originalIndices = dayNumbers.map { mapDayToDisplayIndex(it, viewState.firstDayOfWeek, true) }
        val displayDays = originalIndices.map { reorderedWeekDays[it] }
        val displayDates = originalIndices.map { viewState.dates.getOrNull(it).orEmpty() }
        val todayColumnIndex = originalIndices.indexOf(viewState.todayIndex)

        if (dayNumbers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(bottom = navigationBottom), contentAlignment = Alignment.Center) {
                Column(Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(vectorResource(Res.drawable.calendar_today_24px), null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("这一周暂无课程", style = MaterialTheme.typography.titleLarge)
                    Text("可以切换周次，或显示完整一周添加课程", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    FilledTonalButton(onClick = { showFullWeek = true }, shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.semantics { contentDescription = "显示完整一周" }) { Text("显示完整一周") }
                }
            }
            return@BoxWithConstraints
        }

        val displayDaysCount = displayDays.size
        val minimumDayWidth = (if (displayDaysCount > 5) 36.dp else 48.dp) * maxOf(density.fontScale, style.fontScale, 1f)
        val contentWidth = maxOf(maxWidth, minimumDayWidth * displayDaysCount + style.timeColumnWidth)
        val is24HourMode = style.scheduleMode == ScheduleModeProto.TIME_24H_MODE
        val maxGridSections = if (is24HourMode) 24 else viewState.timeSlots.size

        val totalGridHeight = style.sectionHeight * maxGridSections
        val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        val columnColor = MaterialTheme.colorScheme.surfaceContainerLow
        val todayColumnColor = androidx.compose.ui.graphics.lerp(columnColor, MaterialTheme.colorScheme.primaryContainer, .35f)
        val cellPadding = style.courseBlockOuterPadding.coerceIn(ConnectedScheduleLayout.HalfGap, 6.dp)
        val navigationBottomPx = with(density) { navigationBottom.toPx() }

        val singleSchedulables = remember(viewState.mergedCourses, dayNumbers) {
            calculateSingleSchedulables(viewState.mergedCourses, viewState.firstDayOfWeek, viewState.showWeekends, dayNumbers)
        }
        val sectionHeightPx = with(density) { style.sectionHeight.toPx() }

        var activeDragHour by remember { mutableStateOf<Int?>(null) }
        var activeDragMinuteStr by remember { mutableStateOf<String?>(null) }

        if (is24HourMode && state.expandedItem != null) {
            val currentTargetSection = when {
                state.isTopHandleDragging -> {
                    val minGap = 0.25f
                    val deltaSection = state.topHandleDragOffsetY / sectionHeightPx
                    var proposedStart = state.expandedItem!!.startSection + deltaSection
                    proposedStart = (proposedStart / 0.25f).roundToInt() * 0.25f
                    proposedStart.coerceIn(0f, state.expandedItem!!.endSection - minGap)
                }
                state.isBottomHandleDragging -> {
                    val minGap = 0.25f
                    val deltaSection = state.bottomHandleDragOffsetY / sectionHeightPx
                    var proposedEnd = state.expandedItem!!.endSection + deltaSection
                    proposedEnd = (proposedEnd / 0.25f).roundToInt() * 0.25f
                    proposedEnd.coerceIn(state.expandedItem!!.startSection + minGap, maxGridSections.toFloat())
                }
                state.activeMoveIntent != null -> {
                    val duration = state.activeMoveIntent!!.duration
                    var targetStart = state.activeMoveIntent!!.initialStartSection + (state.bodyDragOffsetY / sectionHeightPx)
                    targetStart = (targetStart / 0.25f).roundToInt() * 0.25f
                    targetStart.coerceIn(0f, maxGridSections - duration)
                }
                else -> null
            }

            if (currentTargetSection != null) {
                val hour = kotlin.math.floor(currentTargetSection).toInt().coerceIn(0, maxGridSections - 1)
                val minuteFraction = currentTargetSection - kotlin.math.floor(currentTargetSection)
                val minute = (minuteFraction * 60).roundToInt() % 60

                activeDragHour = hour
                activeDragMinuteStr = minute.toString().padStart(2, '0')
            } else {
                activeDragHour = null
                activeDragMinuteStr = null
            }
        } else {
            activeDragHour = null
            activeDragMinuteStr = null
        }

        // 自动边缘滚动监测线程
        LaunchedEffect(state.isEditingActive) {
            if (state.isEditingActive) {
                while (true) {
                    val item = state.expandedItem ?: break
                    val threshold = with(density) { 40.dp.toPx() }
                    val scrollSpeed = with(density) { 8.dp.toPx() }

                    var relativeTop: Float? = null
                    var relativeBottom: Float? = null

                    if (state.activeMoveIntent != null) {
                        val currentTopInGrid = item.startSection * sectionHeightPx + state.bodyDragOffsetY
                        val currentBottomInGrid = item.endSection * sectionHeightPx + state.bodyDragOffsetY
                        relativeTop = currentTopInGrid - state.gridScrollState.value
                        relativeBottom = currentBottomInGrid - state.gridScrollState.value
                    } else if (state.isTopHandleDragging) {
                        val currentTopInGrid = item.startSection * sectionHeightPx + state.topHandleDragOffsetY
                        relativeTop = currentTopInGrid - state.gridScrollState.value
                    } else if (state.isBottomHandleDragging) {
                        val currentBottomInGrid = item.endSection * sectionHeightPx + state.bottomHandleDragOffsetY
                        relativeBottom = currentBottomInGrid - state.gridScrollState.value
                    }

                    var scrollAmount = 0f
                    if (relativeTop != null && relativeTop < threshold) {
                        if (state.gridScrollState.value > 0) {
                            scrollAmount = -scrollSpeed
                            if (state.gridScrollState.value + scrollAmount < 0) scrollAmount = -state.gridScrollState.value.toFloat()
                        }
                    } else if (relativeBottom != null && state.viewportHeightPx > 0f && relativeBottom > state.viewportHeightPx - threshold) {
                        val maxScroll = state.gridScrollState.maxValue
                        if (state.gridScrollState.value < maxScroll) {
                            scrollAmount = scrollSpeed
                            if (state.gridScrollState.value + scrollAmount > maxScroll) scrollAmount = (maxScroll - state.gridScrollState.value).toFloat()
                        }
                    }

                    if (scrollAmount != 0f) {
                        state.gridScrollState.scrollBy(scrollAmount)
                        if (state.activeMoveIntent != null) {
                            state.bodyDragOffsetY += scrollAmount
                        } else if (state.isTopHandleDragging) {
                            state.topHandleDragOffsetY += scrollAmount
                        } else if (state.isBottomHandleDragging) {
                            state.bottomHandleDragOffsetY += scrollAmount
                        }
                    }
                    delay(16.milliseconds)
                }
            }
        }

        Column(Modifier.fillMaxSize().horizontalScroll(horizontalScroll).width(contentWidth)) {
            DayHeader(style, displayDays, displayDates, viewState.currentYear, todayColumnIndex, pageTextColor, pageSubTextColor)

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { state.viewportHeightPx = (it.height - navigationBottomPx).coerceAtLeast(0f) }
                    .clipScheduleViewport(style.timeColumnWidth, displayDaysCount)
                    .verticalScroll(state = state.gridScrollState, enabled = state.expandedItem == null)
                    .padding(bottom = navigationBottom)
            ) {
                TimeColumn(
                    style = style, timeSlots = viewState.timeSlots, maxGridSections = maxGridSections,
                    is24HourMode = is24HourMode, onTimeSlotClicked = { actions.onTimeSlotClicked() },
                    modifier = Modifier.height(totalGridHeight),
                    currentSectionIndex = if (viewState.todayIndex < 0 || viewState.mergedCourses.any { block -> block.courses.any { !it.course.isCustomTime && it.course.customStartTime != null } }) -1
                        else if (is24HourMode) courseNow.hour else viewState.timeSlots.firstOrNull {
                            it.startTime <= courseNow.time.toString().take(5) && courseNow.time.toString().take(5) < it.endTime
                        }?.number ?: -1, textColor = pageTextColor,
                    subTextColor = pageSubTextColor,
                    activeDragHour = activeDragHour,
                    activeDragMinuteStr = activeDragMinuteStr,
                    showClockTimes = viewState.mergedCourses.none { block -> block.courses.any {
                        !it.course.isCustomTime && it.course.customStartTime != null && it.course.customEndTime != null
                    } }
                )

                Layout(
                    content = {
                        singleSchedulables.forEach { item ->
                            val isExpanded = state.expandedItem?.courseWrapper?.course?.id == item.courseWrapper.course.id

                            Box(
                                modifier = Modifier
                                    .padding(start = cellPadding, top = cellPadding,
                                        end = if (item.columnIndex == displayDaysCount - 1 && item.subColumnIndex == item.subColumnCount - 1) 0.dp else cellPadding,
                                        bottom = if (item.endSection >= maxGridSections) 0.dp else cellPadding)
                                    .zIndex(if (isExpanded) 2f else 0f)
                                    .then(
                                        if (!isExpanded) {
                                            Modifier.semantics(mergeDescendants = true) {}.combinedClickable(
                                                onClick = { actions.onCourseBlockClicked(item.parentBlock) },
                                                onLongClick = {
                                                    state.expandedItem = item
                                                    actions.onHoldStateChanged(true)
                                                }
                                            )
                                        } else {
                                            Modifier.pointerInput(item) {
                                                detectTapGestures(onTap = { state.expandedItem = null; actions.onHoldStateChanged(false) })
                                            }
                                        }
                                    )
                            ) {
                                CourseBlock(
                                    courseWrapper = item.courseWrapper,
                                    temporalStatus = if (item.parentBlock.isVisualDemoted) null else viewState.weekStartDate?.let { startDate ->
                                        val course = item.courseWrapper.course
                                        val date = startDate.plus(((course.day - viewState.firstDayOfWeek + 7) % 7).toLong(), DateTimeUnit.DAY)
                                        courseTemporalStatus(date, course.customStartTime ?: viewState.timeSlots.find { it.number == course.startSection }?.startTime,
                                            course.customEndTime ?: viewState.timeSlots.find { it.number == course.endSection }?.endTime, courseNow)
                                    },
                                    isVisualDemoted = item.parentBlock.isVisualDemoted,
                                    style = style,
                                    timeSlots = viewState.timeSlots,
                                    isFloating = isExpanded,
                                    blockShape = if (isExpanded) null else ConnectedScheduleLayout.shape(
                                        bottomEnd = item.columnIndex == displayDaysCount - 1 &&
                                            item.subColumnIndex == item.subColumnCount - 1 && item.endSection >= maxGridSections,
                                        inner = style.courseBlockCornerRadius.coerceIn(0.dp, ConnectedScheduleLayout.InnerCorner)),
                                    modifier = if (isExpanded) {
                                        if (!item.parentBlock.isVisualDemoted) {
                                            Modifier.pointerInput(item, state.gridWidthPx) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        state.activeMoveIntent = CourseMoveIntent(item.parentBlock, item.parentBlock.day, item.startSection, item.endSection - item.startSection)
                                                        state.bodyDragOffsetX = 0f; state.bodyDragOffsetY = 0f
                                                    },
                                                    onDragEnd = {
                                                        val intent = state.activeMoveIntent
                                                        if (intent != null && state.gridWidthPx > 0f) {
                                                            val cellWidth = state.gridWidthPx / displayDaysCount

                                                            val initialX = item.columnIndex * cellWidth + (item.subColumnIndex * (cellWidth / item.subColumnCount))

                                                            val currentAbsoluteX = initialX + state.bodyDragOffsetX
                                                            val blockWidth = cellWidth / item.subColumnCount

                                                            val strictThresholdPx = with(density) { (-6.18).dp.toPx() }

                                                            val touchLeftEdge = currentAbsoluteX <= strictThresholdPx
                                                            val touchRightEdge = (currentAbsoluteX + blockWidth) >= (state.gridWidthPx - strictThresholdPx)

                                                            if (touchLeftEdge || touchRightEdge) {
                                                                actions.onInitiateFloatingMode(intent.parentBlock)
                                                                state.resetAllStates()
                                                                actions.onHoldStateChanged(false)
                                                            } else {
                                                                val deltaCols = (state.bodyDragOffsetX / cellWidth).roundToInt()
                                                                val targetDisplayIdx = (item.columnIndex + deltaCols).coerceIn(0, displayDaysCount - 1)
                                                                val targetDay = dayNumbers[targetDisplayIdx]
                                                                var targetStart = intent.initialStartSection + (state.bodyDragOffsetY / sectionHeightPx)
                                                                targetStart = if (is24HourMode) (targetStart / 0.25f).roundToInt() * 0.25f else targetStart.roundToInt().toFloat()
                                                                targetStart = targetStart.coerceIn(0f, maxGridSections - intent.duration)
                                                                val targetEnd = targetStart + intent.duration

                                                                val targetX = targetDisplayIdx * cellWidth
                                                                state.bodyDragOffsetX = targetX - initialX
                                                                state.bodyDragOffsetY = (targetStart - intent.initialStartSection) * sectionHeightPx

                                                                actions.onCourseMovedWithinGrid(intent.parentBlock, targetDay, targetStart, targetEnd)
                                                            }
                                                        }
                                                    },
                                                    onDragCancel = {
                                                        state.resetAllStates()
                                                        actions.onHoldStateChanged(false)
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        state.bodyDragOffsetX += dragAmount.x
                                                        state.bodyDragOffsetY += dragAmount.y
                                                    }
                                                )
                                            }
                                        } else {
                                            Modifier
                                        }
                                    } else Modifier
                                )

                                if (isExpanded && state.activeMoveIntent == null && !item.parentBlock.isVisualDemoted) {
                                    CourseEditHandles(
                                        onDragStart = { isTop ->
                                            if (isTop) {
                                                state.isTopHandleDragging = true
                                                state.isBottomHandleDragging = false
                                                state.topHandleDragOffsetY = 0f
                                            } else {
                                                state.isTopHandleDragging = false
                                                state.isBottomHandleDragging = true
                                                state.bottomHandleDragOffsetY = 0f
                                            }
                                        },
                                        onDragging = { deltaY ->
                                            if (state.isTopHandleDragging) {
                                                val minGap = if (is24HourMode) 0.25f else 1f
                                                val maxAllowedDragY = ((item.endSection - item.startSection) - minGap) * sectionHeightPx

                                                val tentativeOffsetY = state.topHandleDragOffsetY + deltaY
                                                if (is24HourMode) {
                                                    val proposedStart = item.startSection + (tentativeOffsetY / sectionHeightPx)
                                                    state.topHandleDragOffsetY = if (proposedStart > item.endSection - minGap) {
                                                        maxAllowedDragY
                                                    } else {
                                                        tentativeOffsetY
                                                    }
                                                } else {
                                                    state.topHandleDragOffsetY = tentativeOffsetY.coerceAtMost(maxAllowedDragY)
                                                }
                                            } else if (state.isBottomHandleDragging) {
                                                val minGap = if (is24HourMode) 0.25f else 1f
                                                val maxAllowedDragUpY = -(((item.endSection - item.startSection) - minGap) * sectionHeightPx)

                                                val tentativeOffsetY = state.bottomHandleDragOffsetY + deltaY
                                                if (is24HourMode) {
                                                    val proposedEnd = item.endSection + (tentativeOffsetY / sectionHeightPx)
                                                    state.bottomHandleDragOffsetY = if (proposedEnd < item.startSection + minGap) {
                                                        maxAllowedDragUpY
                                                    } else {
                                                        tentativeOffsetY
                                                    }
                                                } else {
                                                    state.bottomHandleDragOffsetY = tentativeOffsetY.coerceAtLeast(maxAllowedDragUpY)
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            val currentItem = state.expandedItem
                                            if (currentItem != null) {
                                                val minGap = if (is24HourMode) 0.25f else 1f
                                                var finalStart = currentItem.startSection
                                                var finalEnd = currentItem.endSection

                                                if (state.isTopHandleDragging) {
                                                    val deltaSection = state.topHandleDragOffsetY / sectionHeightPx
                                                    var proposedStart = currentItem.startSection + deltaSection
                                                    proposedStart = if (is24HourMode) (proposedStart / 0.25f).roundToInt() * 0.25f else proposedStart.roundToInt().toFloat()
                                                    finalStart = proposedStart.coerceIn(0f, finalEnd - minGap)
                                                    state.topHandleDragOffsetY = (finalStart - currentItem.startSection) * sectionHeightPx
                                                } else if (state.isBottomHandleDragging) {
                                                    val deltaSection = state.bottomHandleDragOffsetY / sectionHeightPx
                                                    var proposedEnd = currentItem.endSection + deltaSection
                                                    proposedEnd = if (is24HourMode) (proposedEnd / 0.25f).roundToInt() * 0.25f else proposedEnd.roundToInt().toFloat()
                                                    finalEnd = proposedEnd.coerceIn(finalStart + minGap, maxGridSections.toFloat())
                                                    state.bottomHandleDragOffsetY = (finalEnd - currentItem.endSection) * sectionHeightPx
                                                }
                                                if (finalStart != currentItem.startSection || finalEnd != currentItem.endSection) {
                                                    actions.onCourseTimeAdjusted(currentItem.parentBlock, finalStart, finalEnd)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .height(totalGridHeight).weight(1f)
                        .onSizeChanged { state.gridWidthPx = it.width.toFloat() }
                        .drawBehind {
                            val cellWidth = size.width / displayDaysCount
                            val gutter = ConnectedScheduleLayout.HalfGap.toPx()
                            for (section in 0 until maxGridSections) {
                                for (day in 0 until displayDaysCount) {
                                    val lastDay = day == displayDaysCount - 1
                                    val lastSection = section == maxGridSections - 1
                                    val cellSize = Size((cellWidth - gutter - if (lastDay) 0f else gutter).coerceAtLeast(0f),
                                        (sectionHeightPx - gutter - if (lastSection) 0f else gutter).coerceAtLeast(0f))
                                    val outline = ConnectedScheduleLayout.shape(bottomEnd = lastDay && lastSection)
                                        .createOutline(cellSize, layoutDirection, this)
                                    translate(day * cellWidth + gutter, section * sectionHeightPx + gutter) {
                                        drawOutline(outline, if (day == todayColumnIndex) todayColumnColor else columnColor)
                                        if (!style.hideGridLines) drawOutline(outline, gridLineColor, style = Stroke(.5.dp.toPx()))
                                    }
                                }
                            }
                        }
                        .pointerInput(dayNumbers, sectionHeightPx, viewState.firstDayOfWeek, maxGridSections, is24HourMode, state.expandedItem) {
                            detectTapGestures { offset ->
                                if (state.expandedItem != null) {
                                    state.expandedItem = null
                                    actions.onHoldStateChanged(false)
                                    return@detectTapGestures
                                }
                                val dayIdx = (offset.x / (size.width / displayDaysCount)).toInt().coerceIn(0, displayDaysCount - 1)
                                val secIdx = (offset.y / sectionHeightPx).toInt().coerceIn(0, maxGridSections - 1)
                                actions.onGridCellClicked(dayNumbers[dayIdx], if (is24HourMode) secIdx else (secIdx + 1))
                            }
                        }
                ) { measurables, constraints ->
                    val cellWidth = constraints.maxWidth / displayDaysCount
                    val minGapPx = if (is24HourMode) 0f else with(density) { 30.dp.toPx() }

                    val placeables = measurables.mapIndexed { index, measurable ->
                        val item = singleSchedulables[index]
                        val isExpanded = state.expandedItem?.courseWrapper?.course?.id == item.courseWrapper.course.id

                        val originalHeightPx = ((item.endSection - item.startSection) * sectionHeightPx).toInt()

                        var calculatedHeightPx = originalHeightPx
                        if (isExpanded) {
                            if (state.isTopHandleDragging || state.topHandleDragOffsetY != 0f) {
                                calculatedHeightPx = (originalHeightPx - state.topHandleDragOffsetY.roundToInt()).coerceAtLeast(minGapPx.toInt())
                            } else if (state.isBottomHandleDragging || state.bottomHandleDragOffsetY != 0f) {
                                calculatedHeightPx = (originalHeightPx + state.bottomHandleDragOffsetY.roundToInt()).coerceAtLeast(minGapPx.toInt())
                            }
                        }

                        measurable.measure(
                            Constraints.fixed(
                                (cellWidth / if (isExpanded) 1 else item.subColumnCount),
                                calculatedHeightPx
                            )
                        )
                    }

                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeables.forEachIndexed { index, placeable ->
                            val item = singleSchedulables[index]
                            val isExpanded = state.expandedItem?.courseWrapper?.course?.id == item.courseWrapper.course.id
                            val isMoving = state.activeMoveIntent?.parentBlock?.courses?.any { it.course.id == item.courseWrapper.course.id } == true

                            val originalX = item.columnIndex * cellWidth + (if (isExpanded) 0 else item.subColumnIndex * (cellWidth / item.subColumnCount))
                            val originalY = (item.startSection * sectionHeightPx).toInt()

                            var xPosition = originalX
                            var yPosition = originalY

                            if (isMoving) {
                                val intent = state.activeMoveIntent
                                if (intent != null) {
                                    val intentOriginalX = item.columnIndex * cellWidth
                                    val intentOriginalY = (intent.initialStartSection * sectionHeightPx).toInt()
                                    xPosition = (intentOriginalX + state.bodyDragOffsetX).toInt()
                                    yPosition = (intentOriginalY + state.bodyDragOffsetY).toInt()
                                }
                            } else if (isExpanded) {
                                if (state.isTopHandleDragging || state.topHandleDragOffsetY != 0f) {
                                    val originalHeightPx = ((item.endSection - item.startSection) * sectionHeightPx).toInt()
                                    yPosition = if (originalHeightPx - state.topHandleDragOffsetY >= minGapPx) {
                                        (originalY + state.topHandleDragOffsetY).toInt()
                                    } else {
                                        (originalY + (originalHeightPx - minGapPx)).toInt()
                                    }
                                }
                            }

                            placeable.placeRelative(xPosition, yPosition)
                        }
                    }
                }
            }
        }
    }
}
