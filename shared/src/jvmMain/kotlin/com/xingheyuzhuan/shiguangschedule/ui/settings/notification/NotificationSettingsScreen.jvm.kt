@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import com.xingheyuzhuan.shiguangschedule.ui.settings.ExpressiveSettingsCard as Card
import androidx.compose.material3.CardDefaults
import com.xingheyuzhuan.shiguangschedule.ui.settings.ExpressiveSettingsDivider as HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.dialog_title_set_remind_time
import shiguangschedule.shared.generated.resources.item_course_reminder
import shiguangschedule.shared.generated.resources.item_remind_time_before
import shiguangschedule.shared.generated.resources.label_minutes_input
import shiguangschedule.shared.generated.resources.remind_time_minutes_format
import shiguangschedule.shared.generated.resources.section_title_general

/**
 * 常规设置区域（JVM 平台实现）
 */
@Composable
actual fun PlatformGeneralSettingsSection(
    uiState: NotificationSettingsUiState,
    viewModel: NotificationSettingsViewModel
) {
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
        com.xingheyuzhuan.shiguangschedule.ui.settings.ReminderSettingsOverview(uiState.reminderEnabled,
            uiState.schedulePreviewEnabled, uiState.schedulePreviewHour, viewModel::updateReminderEnabled,
            { viewModel.updateSchedulePreview(it) }, { viewModel.updateSchedulePreview(uiState.schedulePreviewEnabled, it) })
        com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsSection("提醒方式") {
            com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsActionRow(stringResource(Res.string.item_remind_time_before),
                { viewModel.showDialog(NotificationDialogType.EditRemindMinutes) },
                value = stringResource(Res.string.remind_time_minutes_format, uiState.remindBeforeMinutes))
        }
    }
}

/**
 * 通知设置弹窗派发器（JVM 平台实现）
 */
@Composable
actual fun PlatformNotificationDialogDispatcher(
    uiState: NotificationSettingsUiState,
    viewModel: NotificationSettingsViewModel
) {
    when (uiState.activeDialog) {
        is NotificationDialogType.EditRemindMinutes -> {
            var tempInput by remember(uiState.remindBeforeMinutes) {
                mutableStateOf(uiState.remindBeforeMinutes.toString())
            }

            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text(stringResource(Res.string.dialog_title_set_remind_time)) },
                text = {
                    OutlinedTextField(
                        value = tempInput,
                        onValueChange = { input -> tempInput = input.filter { c -> c.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text(stringResource(Res.string.label_minutes_input)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val mins = tempInput.toIntOrNull() ?: 15
                            viewModel.updateRemindBeforeMinutes(mins)
                        }
                    , shapes = androidx.compose.material3.ButtonDefaults.shapes()) {
                        Text(stringResource(Res.string.action_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }, shapes = androidx.compose.material3.ButtonDefaults.shapes()) {
                        Text(stringResource(Res.string.action_cancel))
                    }
                }
            )
        }
        else -> Unit
    }
}