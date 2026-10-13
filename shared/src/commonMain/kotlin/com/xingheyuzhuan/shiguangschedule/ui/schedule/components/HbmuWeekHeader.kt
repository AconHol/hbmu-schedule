// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.*

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun HbmuWeekHeader(weekTitle: String, firstDate: LocalDate, lastDate: LocalDate, isCurrentWeek: Boolean,
    onPrevious: () -> Unit, onNext: () -> Unit, onSelectWeek: () -> Unit, onToday: () -> Unit, onTables: () -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("课程表", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("每周安排", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            IconButton(onTables, shapes = IconButtonDefaults.shapes()) { Icon(vectorResource(Res.drawable.swap_horiz_24px), "切换课表") }
            FilledTonalIconButton(onSettings, shapes = IconButtonDefaults.shapes()) { Icon(vectorResource(Res.drawable.tune_24px), "课表设置") }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalIconButton(onClick = onPrevious, modifier = Modifier.size(48.dp), shapes = IconButtonDefaults.shapes()) { Icon(vectorResource(Res.drawable.arrow_back_24px), "上一周") }
            FilledTonalButton(onClick = onSelectWeek, modifier = Modifier.weight(1f).heightIn(min = 56.dp), shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(weekTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${firstDate.month.ordinal + 1}.${firstDate.day} — ${lastDate.month.ordinal + 1}.${lastDate.day}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(vectorResource(Res.drawable.arrow_drop_down_24px), null, Modifier.size(20.dp))
                }
            }
            FilledTonalIconButton(onClick = onNext, modifier = Modifier.size(48.dp), shapes = IconButtonDefaults.shapes()) { Icon(vectorResource(Res.drawable.chevron_right_24px), "下一周") }
        }
        if (!isCurrentWeek) TextButton(onToday, Modifier.align(Alignment.End).heightIn(min = 48.dp)) { Text("回到本周") }
    }
}
