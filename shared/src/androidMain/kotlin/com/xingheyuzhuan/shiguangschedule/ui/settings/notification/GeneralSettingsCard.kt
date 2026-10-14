// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import com.xingheyuzhuan.shiguangschedule.ui.settings.*

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import com.xingheyuzhuan.shiguangschedule.ui.settings.ExpressiveSettingsCard as Card
import androidx.compose.material3.CardDefaults
import com.xingheyuzhuan.shiguangschedule.ui.settings.ExpressiveSettingsDivider as HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.desc_compat_wearable_sync
import shiguangschedule.shared.generated.resources.item_auto_mode
import shiguangschedule.shared.generated.resources.item_background_and_autostart
import shiguangschedule.shared.generated.resources.item_compat_wearable_sync
import shiguangschedule.shared.generated.resources.item_course_reminder
import shiguangschedule.shared.generated.resources.item_dnd_permission
import shiguangschedule.shared.generated.resources.item_exact_alarm_permission
import shiguangschedule.shared.generated.resources.item_ignore_battery_optimization
import shiguangschedule.shared.generated.resources.item_remind_time_before
import shiguangschedule.shared.generated.resources.remind_time_minutes_format
import shiguangschedule.shared.generated.resources.section_title_general
import shiguangschedule.shared.generated.resources.status_authorized
import shiguangschedule.shared.generated.resources.status_disabled
import shiguangschedule.shared.generated.resources.status_enabled
import shiguangschedule.shared.generated.resources.status_unauthorized
import shiguangschedule.shared.generated.resources.text_auto_mode_dependency
import shiguangschedule.shared.generated.resources.text_permission_importance_detail
import shiguangschedule.shared.generated.resources.text_permission_importance_title

/**
 * 常规设置卡片 UI 组件 (Android 专属)
 */
@Composable
fun GeneralSettingsCard(
    uiState: NotificationSettingsUiState,
    currentModeText: String?,
    onReminderToggle: (Boolean) -> Unit,
    onPreviewToggle: (Boolean) -> Unit,
    onPreviewHourChange: (Int) -> Unit,
    onNotificationPermissionClick: () -> Unit,
    onCompatWearableToggle: (Boolean) -> Unit,
    onAutoModeClick: () -> Unit,
    onRemindTimeClick: () -> Unit,
    onAppSettingsClick: () -> Unit,
    onBatteryOptimizationClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ReminderSettingsOverview(uiState.reminderEnabled, uiState.schedulePreviewEnabled, uiState.schedulePreviewHour,
            onReminderToggle, onPreviewToggle, onPreviewHourChange)
        if (!uiState.notificationAllowed) SettingsActionRow("允许发送通知", onNotificationPermissionClick,
            "授予通知权限后，上课提醒和明日预告才能送达。")
        SettingsSection("提醒方式") {
            SettingsActionRow(stringResource(Res.string.item_auto_mode), onAutoModeClick,
                if (!uiState.reminderEnabled) stringResource(Res.string.text_auto_mode_dependency) else null,
                value = currentModeText, position = 0, count = 3)
            SettingsActionRow(stringResource(Res.string.item_remind_time_before), onRemindTimeClick,
                value = stringResource(Res.string.remind_time_minutes_format, uiState.remindBeforeMinutes), position = 1, count = 3)
            SettingsToggleRow(stringResource(Res.string.item_compat_wearable_sync), uiState.compatWearableSync,
                onCompatWearableToggle, stringResource(Res.string.desc_compat_wearable_sync), 2, 3)
        }
        SettingsSection("系统权限与后台运行") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) SettingsActionRow("准时提醒", { openExactAlarmSettings(context) },
                "开启后可减少系统延后通知。", value = if (uiState.exactAlarmStatus) "已开启" else "未开启", position = 0, count = 4)
            SettingsActionRow(stringResource(Res.string.item_dnd_permission), { openDndSettings(context) },
                value = if (uiState.dndPermissionStatus) "已授权" else "未授权", position = 1, count = 4)
            SettingsActionRow(stringResource(Res.string.item_background_and_autostart), onAppSettingsClick, position = 2, count = 4)
            SettingsActionRow(stringResource(Res.string.item_ignore_battery_optimization), onBatteryOptimizationClick, position = 3, count = 4)
        }
    }
}
