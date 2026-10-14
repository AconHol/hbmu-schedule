// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.ui.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.testTag
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTemporalStatus
import com.xingheyuzhuan.shiguangschedule.ui.components.courseTemporalStatus
import com.xingheyuzhuan.shiguangschedule.ui.components.rememberCourseNow
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalNavigationContentBottom
import com.xingheyuzhuan.shiguangschedule.ui.components.connectedCourseShape
import com.xingheyuzhuan.shiguangschedule.hbmu.HbmuBrandHeader
import com.xingheyuzhuan.shiguangschedule.hbmu.HbmuRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.*
import kotlin.time.Clock

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun TodayScheduleScreen(onNavigate: (Destination) -> Unit, onBack: () -> Unit,
    viewModel: TodayScheduleViewModel = koinViewModel(), hbmu: HbmuRepository = koinInject()) {
    val uiState by viewModel.uiState.collectAsState()
    val connection by hbmu.uiState.collectAsState()
    val tomorrow by viewModel.tomorrowCourses.collectAsState()
    val now = rememberCourseNow()
    val time = now.time
    val gridStyle by viewModel.gridStyle.collectAsState()
    val styles: StyleSettingsRepository = koinInject()
    var detail by remember { mutableStateOf<CourseDisplayModel?>(null) }
    val scope = rememberCoroutineScope()
    val today = now.date
    val state = uiState as? TodayUiState.Success
    val next = state?.courses?.firstOrNull { it.endTime == null || it.endTime!! > time.toString().take(5) }
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = LocalNavigationContentBottom.current + 20.dp)) {
            item { HbmuBrandHeader("今日课表", "${today.month.ordinal + 1}月${today.day}日 · 星期${"一二三四五六日"[today.dayOfWeek.isoDayNumber - 1]}", { onNavigate(Destination.Settings) }) }
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (next != null && courseTemporalStatus(today, next.startTime, next.endTime, now) == CourseTemporalStatus.ONGOING) "正在上课" else if (next != null) "下一节课" else "今日安排", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .14f), contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                                Text(if (state != null && state.weekIndex > 0) "第 ${state.weekIndex} 周" else "HBMU", Modifier.padding(horizontal = 14.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Text(next?.course?.name ?: if (connection.lastSync == 0L) "连接你的课表" else if (state?.courses?.isNotEmpty() == true) "今天的课，已完成" else "今天，留点空白", style = MaterialTheme.typography.headlineMedium)
                        Text(if (next != null) "${next.startTime ?: "时间待设置"} — ${next.endTime ?: ""} · ${next.course.position.ifBlank { "教室待公布" }}" else if (connection.lastSync == 0L) "登录教务，自动同步整学期课程" else "今天的课程已经结束，或暂无排课", style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = { if (next != null) detail = next else onNavigate(if (connection.lastSync == 0L) Destination.Hbmu else Destination.CourseSchedule) }, modifier = Modifier.heightIn(min = 48.dp), shapes = ButtonDefaults.shapes()) {
                            Text(if (next != null) "查看课程详情" else if (connection.lastSync == 0L) "连接 HBMU 教务" else "查看本周课表")
                            Spacer(Modifier.width(8.dp)); Icon(vectorResource(Res.drawable.chevron_right_24px), null)
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = { onNavigate(Destination.CourseSchedule) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shapes = ButtonDefaults.shapes()) {
                        Icon(vectorResource(Res.drawable.view_week_24px), null); Spacer(Modifier.width(8.dp)); Text("本周课表")
                    }
                    OutlinedButton(onClick = { if (!connection.signedIn) onNavigate(Destination.Hbmu) else scope.launch { hbmu.sync() } }, modifier = Modifier.weight(1f).heightIn(min = 52.dp), enabled = !connection.busy, shapes = ButtonDefaults.shapes()) {
                        Icon(vectorResource(Res.drawable.refresh_24px), null); Spacer(Modifier.width(8.dp)); Text(if (connection.busy) "同步中" else "同步课表")
                    }
                }
            }
            item {
                Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("当天课程", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    Text("${state?.courses?.size ?: 0} 节安排", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (state == null) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) }
            if (state != null && state.courses.isEmpty()) item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.fillMaxWidth().padding(28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(vectorResource(Res.drawable.calendar_today_24px), null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(if (state.status == TodayStatus.NoSemesterConfig) "从连接教务开始" else "暂无课程安排", style = MaterialTheme.typography.titleMedium)
                        Text(if (state.status == TodayStatus.NoSemesterConfig) "同步后会自动设置学期周次，你也可以在工具中手动导入课表。" else "左右滑动周课表，看看接下来几天的安排。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                val courses = state?.courses.orEmpty()
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                courses.forEachIndexed { index, model ->
                val courseState = courseTemporalStatus(today, model.startTime, model.endTime, now)
                val finished = courseState == CourseTemporalStatus.COMPLETED
                val ongoing = courseState == CourseTemporalStatus.ONGOING
                Surface(onClick = { detail = model }, modifier = Modifier.fillMaxWidth(), shape = connectedCourseShape(index, courses.size), color = if (ongoing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer, contentColor = if (ongoing) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer) {
                    Row(Modifier.heightIn(min = 92.dp).padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.width(60.dp)) {
                            Text(model.startTime ?: "待设置", style = MaterialTheme.typography.titleMedium)
                            Text(model.endTime ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text(if (ongoing) "正在上课" else if (finished) "已结束" else "第${model.course.startSection ?: ""}—${model.course.endSection ?: ""}节", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .72f))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(model.course.name, modifier = Modifier.testTag("today-course-title"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textDecoration = if (finished && gridStyle.strikeCompletedToday) TextDecoration.LineThrough else TextDecoration.None)
                            Text(listOf(model.course.position.ifBlank { "教室待公布" }, model.course.teacher).filter { it.isNotBlank() }.joinToString(" · "), modifier = Modifier.testTag("today-course-details"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .78f))
                        }
                        Icon(vectorResource(Res.drawable.chevron_right_24px), null, tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .70f))
                    }
                }
                }
                }
            }
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("明日预告", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${tomorrow.size} 堂课", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        if (tomorrow.isEmpty()) Text(if (connection.lastSync == 0L) "同步课表后，就能提前看到明天的安排。" else "明天暂无课程安排", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        tomorrow.take(3).forEach { course ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                                Text(course.startTime, Modifier.width(48.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f)) {
                                    Text(course.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(course.position.ifBlank { "教室待公布" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                    }
                }
            }
            if (!gridStyle.hideReminderTip) item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("让课表及时提醒你", style = MaterialTheme.typography.titleSmall)
                            Text("开启上课提醒与每日预告，提前安排下一堂课。", style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { onNavigate(Destination.NotificationSettings) }, shapes = ButtonDefaults.shapes()) { Text("设置上课提醒与每日预告") }
                        }
                        IconButton(onClick = { scope.launch { styles.setHideReminderTip(true) } }, shapes = IconButtonDefaults.shapes()) {
                            Icon(vectorResource(Res.drawable.close_24px), "关闭提醒设置提示")
                        }
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (connection.lastSync > 0) "已保存本地课表 · 离线也能查看" else "课程表 · 安排好每一天", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    detail?.let { model -> AlertDialog(onDismissRequest = { detail = null }, title = { Text(model.course.name) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${model.startTime ?: "时间待设置"} — ${model.endTime ?: ""}")
            Text(model.course.position.ifBlank { "教室待公布" }); Text(model.course.teacher)
            model.course.remark?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }, confirmButton = { TextButton({ detail = null }) { Text("知道了") } }, dismissButton = { TextButton({ detail = null; onNavigate(Destination.AddEditCourse(model.course.id)) }) { Text("编辑课程") } }) }
}
