// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.*

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun HbmuWeekHeader(weekTitle: String, firstDate: LocalDate, lastDate: LocalDate, isCurrentWeek: Boolean,
    onPrevious: () -> Unit, onNext: () -> Unit, onSelectWeek: () -> Unit, onToday: () -> Unit, onTables: () -> Unit, onSettings: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val monthLabel = when {
        firstDate.year != lastDate.year -> "${firstDate.year}年${firstDate.month.number}月—${lastDate.year}年${lastDate.month.number}月"
        firstDate.month != lastDate.month -> "${firstDate.year}年${firstDate.month.number}—${lastDate.month.number}月"
        else -> "${firstDate.year}年${firstDate.month.number}月"
    }
    val controlColors = ButtonDefaults.filledTonalButtonColors(
        containerColor = colors.surfaceContainerHigh,
        contentColor = colors.onSurfaceVariant
    )
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = ConnectedScheduleLayout.PageMargin)
        .padding(top = 8.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("周课表", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(monthLabel, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // Keep the toolbar height stable when browsing another week.
            IconButton(onToday, modifier = Modifier.size(48.dp), shapes = IconButtonDefaults.shapes()) {
                Icon(vectorResource(Res.drawable.calendar_today_24px), "回到本周",
                    tint = if (isCurrentWeek) colors.primary else colors.onSurfaceVariant)
            }
            IconButton(onTables, modifier = Modifier.size(48.dp), shapes = IconButtonDefaults.shapes()) {
                Icon(vectorResource(Res.drawable.swap_horiz_24px), "切换课表", tint = colors.onSurfaceVariant)
            }
            IconButton(onSettings, modifier = Modifier.size(48.dp), shapes = IconButtonDefaults.shapes()) {
                Icon(vectorResource(Res.drawable.tune_24px), "课表设置", tint = colors.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ConnectedScheduleLayout.Gap)) {
            FilledTonalButton(onClick = onPrevious, modifier = Modifier.width(48.dp).fillMaxHeight(),
                shapes = ButtonDefaults.shapes(
                    shape = ConnectedScheduleLayout.shape(topStart = true, bottomStart = true),
                    pressedShape = ConnectedScheduleLayout.shape(topStart = true, bottomStart = true, inner = 4.dp, outer = 22.dp)),
                colors = controlColors,
                contentPadding = PaddingValues(0.dp)) { Icon(vectorResource(Res.drawable.chevron_right_24px), "上一周", Modifier.rotate(180f)) }
            FilledTonalButton(onClick = onSelectWeek, modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                .semantics { contentDescription = "选择周次" },
                shapes = ButtonDefaults.shapes(shape = ConnectedScheduleLayout.shape(), pressedShape = ConnectedScheduleLayout.shape(inner = 4.dp)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = controlColors) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(20.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(weekTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface)
                        Text("${firstDate.month.number}.${firstDate.day} — ${lastDate.month.number}.${lastDate.day}",
                            style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                    Icon(vectorResource(Res.drawable.arrow_drop_down_24px), null, Modifier.size(20.dp))
                }
            }
            FilledTonalButton(onClick = onNext, modifier = Modifier.width(48.dp).fillMaxHeight(),
                shapes = ButtonDefaults.shapes(
                    shape = ConnectedScheduleLayout.shape(topEnd = true, bottomEnd = true),
                    pressedShape = ConnectedScheduleLayout.shape(topEnd = true, bottomEnd = true, inner = 4.dp, outer = 22.dp)),
                colors = controlColors,
                contentPadding = PaddingValues(0.dp)) { Icon(vectorResource(Res.drawable.chevron_right_24px), "下一周") }
        }
    }
}
