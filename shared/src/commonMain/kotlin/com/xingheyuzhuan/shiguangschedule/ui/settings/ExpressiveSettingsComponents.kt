@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.xingheyuzhuan.shiguangschedule.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.ui.components.connectedCourseShape
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.*

/** Shared settings presentation; callbacks, permissions and data remain owned by each screen. */
@Composable
fun ExpressiveSettingsTopAppBar(title: @Composable () -> Unit, modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {}, actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
    scrollBehavior: TopAppBarScrollBehavior? = null) {
    MediumFlexibleTopAppBar(title = title, modifier = modifier, navigationIcon = navigationIcon,
        actions = actions, colors = colors, scrollBehavior = scrollBehavior)
}

@Composable
fun ExpressiveSettingsIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    interactionSource: MutableInteractionSource? = null, content: @Composable () -> Unit) {
    FilledTonalIconButton(onClick = onClick, modifier = modifier, enabled = enabled, colors = colors, interactionSource = interactionSource,
        shapes = IconButtonDefaults.shapes(), content = content)
}

@Composable
fun ExpressiveSettingsListItem(headlineContent: @Composable () -> Unit, modifier: Modifier = Modifier,
    overlineContent: @Composable (() -> Unit)? = null, supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null, trailingContent: @Composable (() -> Unit)? = null,
    colors: ListItemColors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    tonalElevation: Dp = 0.dp, shadowElevation: Dp = 0.dp) {
    // Let the caller's container or ListItem paint the background once. A second Surface
    // with inset padding exposes a differently coloured rim in nested settings cards.
    ListItem(headlineContent,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).then(modifier),
        overlineContent = overlineContent, supportingContent = supportingContent,
        leadingContent = leadingContent, trailingContent = trailingContent, colors = colors,
        tonalElevation = tonalElevation, shadowElevation = shadowElevation)
}

@Composable
fun ExpressiveSettingsDivider(modifier: Modifier = Modifier, thickness: Dp = 1.dp,
    color: Color = MaterialTheme.colorScheme.outlineVariant) { Spacer(modifier.height(4.dp)) }

@Composable
fun ExpressiveSettingsCard(modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.extraLarge,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit) {
    Card(modifier, shape, colors, elevation, border, content)
}

@Composable
fun ExpressiveSettingsCard(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(onClick, modifier, enabled, shape, colors, elevation, border, interactionSource, content)
}

@Composable
fun SettingsSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, Modifier.padding(start = 8.dp, top = 12.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
fun SettingsActionRow(title: String, onClick: () -> Unit, description: String? = null,
    icon: ImageVector? = null, value: String? = null, position: Int = 0, count: Int = 1,
    trailing: @Composable (() -> Unit)? = null) {
    Surface(onClick, Modifier.fillMaxWidth(), shape = connectedCourseShape(position, count),
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        ListItem(headlineContent = { Text(title, style = MaterialTheme.typography.titleSmall) },
            supportingContent = description?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
            leadingContent = icon?.let { { Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { Icon(it, null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
            } } },
            trailingContent = trailing ?: { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                value?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                Icon(vectorResource(Res.drawable.chevron_right_24px), null)
            } }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
    }
}

@Composable
fun SettingsToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, description: String? = null,
    position: Int = 0, count: Int = 1) {
    Surface(Modifier.fillMaxWidth().clip(connectedCourseShape(position, count))
        .toggleable(checked, role = Role.Switch, onValueChange = onChange),
        shape = connectedCourseShape(position, count), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        ListItem(headlineContent = { Text(title, style = MaterialTheme.typography.titleSmall) },
            supportingContent = description?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
            trailingContent = { Switch(checked, null, thumbContent = if (checked) ({ Icon(vectorResource(Res.drawable.check_24px), null, Modifier.size(SwitchDefaults.IconSize)) }) else null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent))
    }
}

@Composable
fun ReminderSettingsOverview(reminderEnabled: Boolean, previewEnabled: Boolean, previewHour: Int,
    onReminderToggle: (Boolean) -> Unit, onPreviewToggle: (Boolean) -> Unit, onHourChange: (Int) -> Unit) {
    SettingsSection("上课提醒与每日预告") {
        SettingsToggleRow("上课提醒", reminderEnabled, onReminderToggle, "通知会附带时间、教室与老师，点击即可回到课表。", 0, 2)
        SettingsToggleRow("明日课表预告", previewEnabled, onPreviewToggle, "前一晚通知明天的课程，空课表不打扰。", 1, 2)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(18, 20, 21).forEachIndexed { index, hour ->
                FilledTonalButton(onClick = { onHourChange(hour) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    enabled = previewEnabled, shapes = ButtonDefaults.shapes(shape = connectedCourseShape(index, 3)),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = if (previewHour == hour) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (previewHour == hour) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)) { Text("$hour:00") }
            }
        }
        Text("使用课程自身的起止时间；没有单独时间的课程使用本地作息。", Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
