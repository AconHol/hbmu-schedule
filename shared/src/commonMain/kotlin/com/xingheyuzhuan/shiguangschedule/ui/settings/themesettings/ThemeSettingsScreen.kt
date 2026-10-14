package com.xingheyuzhuan.shiguangschedule.ui.settings.themesettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.model.AppThemeMode
import com.xingheyuzhuan.shiguangschedule.ui.components.AdvancedColorPicker
import com.xingheyuzhuan.shiguangschedule.ui.components.ColorPickerConfig
import com.xingheyuzhuan.shiguangschedule.ui.components.connectedCourseShape
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsViewModel
import com.xingheyuzhuan.shiguangschedule.ui.theme.supportsDynamicColor
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeSettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val settings by viewModel.uiState.collectAsState()
    val current = settings.appSettings
    var editingDark by remember { mutableStateOf<Boolean?>(null) }
    val useWallpaper = supportsDynamicColor && current.useDynamicColor
    Scaffold(topBar = {
        TopAppBar(title = { Text("主题与颜色") }, navigationIcon = {
            FilledTonalIconButton(onClick = onBack) { Icon(vectorResource(Res.drawable.arrow_back_24px), "返回") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("让课表有你的颜色", style = MaterialTheme.typography.headlineSmall)
                    Text(if (useWallpaper) "当前跟随系统壁纸配色" else "当前使用自定义配色", style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary).forEach { color ->
                            Box(Modifier.size(40.dp).background(color, CircleShape))
                        }
                    }
                }
            }
            Text("显示模式", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AppThemeMode.entries.forEach { mode ->
                    ToggleButton(checked = current.themeMode == mode, onCheckedChange = { if (it) viewModel.onThemeModeChanged(mode) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                        Text(stringResource(mode.labelRes), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Text("配色方式", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(onClick = { viewModel.onUseDynamicColorChanged(true) }, enabled = supportsDynamicColor, shape = connectedCourseShape(0, 2), color = MaterialTheme.colorScheme.surfaceContainer) {
                    ListItem(headlineContent = { Text("壁纸动态取色") }, supportingContent = { Text(if (supportsDynamicColor) "使用系统生成的浅色和深色配色" else "需要 Android 12 或更高版本") }, trailingContent = { RadioButton(selected = useWallpaper, onClick = null, enabled = supportsDynamicColor) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
                }
                Surface(onClick = { viewModel.onUseDynamicColorChanged(false) }, shape = connectedCourseShape(1, 2), color = MaterialTheme.colorScheme.surfaceContainer) {
                    ListItem(headlineContent = { Text("自定义颜色") }, supportingContent = { Text("分别选择浅色和深色主题主色") }, trailingContent = { RadioButton(selected = !useWallpaper, onClick = null) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
                }
            }
            if (!useWallpaper) {
                Text("推荐主色", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("湖绿" to 0xFF285E50, "蓝色" to 0xFF385A9E, "紫色" to 0xFF7953A9, "玫瑰" to 0xFF9C465C, "金色" to 0xFF805D19).forEach { (name, argb) ->
                        val color = Color(argb)
                        val isSelected = Color(current.customPrimaryColor.light) == color && Color(current.customPrimaryColor.dark) == color
                        Surface(onClick = { viewModel.onCustomPrimaryChanged(color) }, modifier = Modifier.size(48.dp).semantics { contentDescription = "$name 主色"; selected = isSelected }.border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape), shape = CircleShape, color = color) {
                            Box(contentAlignment = androidx.compose.ui.Alignment.Center) { if (isSelected) Text("✓", color = Color.White) }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(false, true).forEachIndexed { index, dark ->
                        val color = Color(if (dark) current.customPrimaryColor.dark else current.customPrimaryColor.light)
                        Surface(onClick = { editingDark = dark }, shape = connectedCourseShape(index, 2), color = MaterialTheme.colorScheme.surfaceContainer) {
                            ListItem(headlineContent = { Text(if (dark) "深色主题主色" else "浅色主题主色") }, supportingContent = { Text("自定义色相或输入十六进制颜色") }, trailingContent = { Box(Modifier.size(36.dp).background(color, CircleShape)) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
                        }
                    }
                }
                TextButton(onClick = { viewModel.onCustomPrimaryChanged() }) { Text("恢复默认配色") }
            }
            Text("主色会生成完整的主题色阶，按钮、课表、底栏和弹窗会一起更新。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        }
    }
    editingDark?.let { dark ->
        val initial = Color(if (dark) current.customPrimaryColor.dark else current.customPrimaryColor.light)
        var draft by remember(dark, initial) { mutableStateOf(initial) }
        ModalBottomSheet(onDismissRequest = { editingDark = null }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (dark) "深色主题主色" else "浅色主题主色", style = MaterialTheme.typography.headlineSmall)
                AdvancedColorPicker(initialColor = initial, onColorChanged = { draft = it }, config = ColorPickerConfig(showAlpha = false, showInputMode = true))
                Button(onClick = {
                    if (dark) viewModel.onCustomDarkPrimaryChanged(draft) else viewModel.onCustomLightPrimaryChanged(draft)
                    editingDark = null
                }, modifier = Modifier.fillMaxWidth(), shapes = ButtonDefaults.shapes()) { Text("应用颜色") }
            }
        }
    }
}
