// Modified by AconHol (2026): independent learning release and privacy notices.
package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsSection
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsActionRow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.xingheyuzhuan.shiguangschedule.ui.settings.ExpressiveSettingsCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.xingheyuzhuan.shiguangschedule.ui.settings.ExpressiveSettingsIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.xingheyuzhuan.shiguangschedule.ui.settings.ExpressiveSettingsTopAppBar as TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.tool.UpdateChecker
import com.xingheyuzhuan.shiguangschedule.tool.UpdatePlatform
import com.xingheyuzhuan.shiguangschedule.tool.UpdateStatus
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.qualifier.named
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.app_name
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.code_24px
import shiguangschedule.shared.generated.resources.home_24px
import shiguangschedule.shared.generated.resources.item_check_software_update
import shiguangschedule.shared.generated.resources.item_contributors
import shiguangschedule.shared.generated.resources.item_github_repo
import shiguangschedule.shared.generated.resources.item_language_settings
import shiguangschedule.shared.generated.resources.item_open_source_licenses
import shiguangschedule.shared.generated.resources.item_start_screen_settings
import shiguangschedule.shared.generated.resources.item_update_repo
import shiguangschedule.shared.generated.resources.label_version_prefix
import shiguangschedule.shared.generated.resources.language_24px
import shiguangschedule.shared.generated.resources.list_alt_24px
import shiguangschedule.shared.generated.resources.palette_24px
import shiguangschedule.shared.generated.resources.people_alt_24px
import shiguangschedule.shared.generated.resources.theme_settings_title
import shiguangschedule.shared.generated.resources.title_more_options
import shiguangschedule.shared.generated.resources.update_24px

private const val GITHUB_REPO_URL = "https://github.com/AconHol/hbmu-schedule"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
    updateChecker: UpdateChecker = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current

    // 从 Koin 动态获取注入的版本号
    val appVersionName: String = koinInject(named("AppVersionName"))

    // 状态观察
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDeveloperModeEnabled = uiState.appSettings.developerModeEnabled

    // 更新逻辑相关状态
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    var selectedPlatform by remember { mutableStateOf(UpdatePlatform.GITHUB) }

    // 弹窗可见性控制
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showChannelDialog by remember { mutableStateOf(false) }
    var showStartScreenDialog by remember { mutableStateOf(false) }

    // 逻辑：执行更新检查
    val startUpdateCheck: (UpdatePlatform) -> Unit = { platform ->
        selectedPlatform = platform
        showChannelDialog = false
        updateStatus = UpdateStatus.Checking
        showUpdateDialog = true
        coroutineScope.launch {
            updateStatus = updateChecker.checkUpdate(platform, appVersionName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.title_more_options)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.arrow_back_24px),
                            contentDescription = stringResource(Res.string.a11y_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp)) {
                DynamicAppIconHeader(isDeveloperModeEnabled, { viewModel.onDeveloperModeChanged(true) }, Modifier.size(80.dp))
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    androidx.compose.material3.Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(stringResource(Res.string.label_version_prefix, appVersionName), Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
                if (isDeveloperModeEnabled) DeveloperModeSettingItem(isDeveloperModeEnabled = isDeveloperModeEnabled, onDeveloperModeChanged = { viewModel.onDeveloperModeChanged(it) })
                SettingsSection("应用偏好") {
                    SettingsActionRow(stringResource(Res.string.item_check_software_update), { showChannelDialog = true }, icon = vectorResource(Res.drawable.update_24px), position = 0, count = 4)
                    SettingsActionRow(stringResource(Res.string.item_language_settings), { onNavigate(Destination.LanguageSettings) }, icon = vectorResource(Res.drawable.language_24px), position = 1, count = 4)
                    SettingsActionRow(stringResource(Res.string.theme_settings_title), { onNavigate(Destination.ThemeSettings) }, icon = vectorResource(Res.drawable.palette_24px), position = 2, count = 4)
                    SettingsActionRow(stringResource(Res.string.item_start_screen_settings), { showStartScreenDialog = true }, icon = vectorResource(Res.drawable.home_24px), value = stringResource(uiState.appSettings.startScreen.labelRes), position = 3, count = 4)
                }
                SettingsSection("项目与支持") {
                    SettingsActionRow(stringResource(Res.string.item_github_repo), { uriHandler.openUri(GITHUB_REPO_URL) }, icon = vectorResource(Res.drawable.code_24px), position = 0, count = 4)
                    SettingsActionRow(stringResource(Res.string.item_open_source_licenses), { onNavigate(Destination.OpenSourceLicenses) }, icon = vectorResource(Res.drawable.list_alt_24px), position = 1, count = 4)
                    SettingsActionRow(stringResource(Res.string.item_update_repo), { onNavigate(Destination.UpdateRepo) }, icon = vectorResource(Res.drawable.update_24px), position = 2, count = 4)
                    SettingsActionRow(stringResource(Res.string.item_contributors), { onNavigate(Destination.ContributionList) }, icon = vectorResource(Res.drawable.people_alt_24px), position = 3, count = 4)
                }
                AcknowledgmentContent()
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // --- 弹窗逻辑 ---

    // 启动页切换弹窗
    StartScreenSelectionDialog(
        showDialog = showStartScreenDialog,
        currentSelected = uiState.appSettings.startScreen,
        onDismiss = { showStartScreenDialog = false },
        onConfirm = {
            viewModel.onStartScreenChanged(it)
            showStartScreenDialog = false
        }
    )

    // 检查更新结果弹窗
    UpdateResultDialog(
        showDialog = showUpdateDialog,
        updateStatus = updateStatus,
        onDismiss = {
            showUpdateDialog = false
            if (updateStatus !is UpdateStatus.Found) updateStatus = UpdateStatus.Idle
        },
        onDownloadClick = { targetUrl ->
            updateChecker.launchUpdate(targetUrl)
        }
    )

    // 更新渠道选择弹窗
    ChannelSelectionDialog(
        showDialog = showChannelDialog,
        currentSelected = selectedPlatform,
        onDismiss = { showChannelDialog = false },
        onConfirm = startUpdateCheck
    )
}
