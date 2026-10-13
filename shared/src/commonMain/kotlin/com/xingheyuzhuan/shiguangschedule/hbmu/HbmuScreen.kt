// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.hbmu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import shiguangschedule.shared.generated.resources.*
import kotlin.time.Instant

@Composable
expect fun HbmuCaptchaImage(bytes: ByteArray, modifier: Modifier = Modifier)

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun HbmuBrandHeader(title: String, subtitle: String, onTools: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("课程表 / HBMU", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalIconButton(onClick = onTools, modifier = Modifier.size(48.dp), shapes = IconButtonDefaults.shapes()) {
            Icon(vectorResource(Res.drawable.tune_24px), "工具与设置")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HbmuScreen(onNavigate: (Destination) -> Unit, repository: HbmuRepository = koinInject()) {
    val state by repository.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var account by remember(state.account) { mutableStateOf(state.account) }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var visiblePassword by remember { mutableStateOf(false) }
    var rememberLogin by remember(state.remember) { mutableStateOf(state.remember) }
    var autoSync by remember(state.autoSync) { mutableStateOf(state.autoSync) }
    var termMenu by remember { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(!state.signedIn) }
    var showSignOut by remember { mutableStateOf(false) }
    LaunchedEffect(state.signedIn) { showLogin = !state.signedIn }
    LaunchedEffect(state.captcha) { code = state.captcha?.recognized.orEmpty() }

    Scaffold { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 112.dp)
        ) {
            item { HbmuBrandHeader("我的", "湖北医药学院 · 课表与账号", { onNavigate(Destination.Settings) }) }
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(60.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (state.signedIn) Text(state.displayName.firstOrNull()?.toString() ?: "湖", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
                                    else Icon(vectorResource(Res.drawable.account_circle_24px), null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(if (state.signedIn) state.displayName.ifBlank { "湖医学子" } else "连接你的教务账号", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(if (state.signedIn) "学号 ${state.account}" else "登录一次，课程自动同步", style = MaterialTheme.typography.bodyMedium)
                                Text("湖北医药学院", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .75f))
                            }
                        }
                        if (state.signedIn) {
                            Text(if (state.busy) "正在更新课表…" else if (state.connected) "教务已连接 · 本地课表可用" else "账号已绑定 · 本地课表可用", style = MaterialTheme.typography.labelLarge)
                        } else Text("你的课程、周次和教室，一次同步。", style = MaterialTheme.typography.bodyMedium)
                        if (state.lastSync > 0) {
                            val date = Instant.fromEpochMilliseconds(state.lastSync).toLocalDateTime(TimeZone.currentSystemDefault())
                            Text("上次同步 ${date.date} ${date.hour.toString().padStart(2, '0')}:${date.minute.toString().padStart(2, '0')} · ${state.recordCount} 次排课",
                                style = MaterialTheme.typography.labelMedium)
                        }
                        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        if (state.signedIn && state.busy) Text(state.message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (state.error != null) item {
                Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer) {
                    Column(Modifier.padding(16.dp)) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                        if (state.signedIn) TextButton({ showLogin = true }) { Text("重新登录") }
                    }
                }
            }
            if (state.signedIn) item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("我的学期", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (state.terms.isNotEmpty()) {
                            Box {
                                OutlinedButton(onClick = { termMenu = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                                    Text(state.terms.firstOrNull { it.code == state.semester }?.label ?: state.semester)
                                    Spacer(Modifier.weight(1f)); Icon(vectorResource(Res.drawable.arrow_drop_down_24px), null)
                                }
                                DropdownMenu(expanded = termMenu, onDismissRequest = { termMenu = false }) {
                                    state.terms.forEach { term -> DropdownMenuItem(text = { Text(term.label) }, onClick = {
                                        termMenu = false; scope.launch { repository.selectSemester(term.code); repository.sync() }
                                    }) }
                                }
                            }
                        } else {
                            val year = state.semester.take(4).toIntOrNull()
                            Text(if (year != null) "$year—${year + 1} 学年 · ${if (state.semester.endsWith("01")) "第一" else "第二"}学期" else "同步后读取学校学期", style = MaterialTheme.typography.bodyLarge)
                        }
                        Button(onClick = { scope.launch { repository.sync() } }, enabled = !state.busy && state.account.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Icon(vectorResource(Res.drawable.refresh_24px), null); Spacer(Modifier.width(8.dp)); Text("同步整学期课表")
                        }
                    }
                }
            }
            if (showLogin || !state.signedIn) item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("登录教务", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(account, { account = it.trim() }, label = { Text("学号") }, singleLine = true, enabled = !state.busy,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(password, { password = it }, label = { Text("教务密码") }, singleLine = true, enabled = !state.busy,
                            visualTransformation = if (visiblePassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { TextButton(onClick = { visiblePassword = !visiblePassword }) { Text(if (visiblePassword) "隐藏" else "显示") } })
                        AnimatedVisibility(state.captcha != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    state.captcha?.let { HbmuCaptchaImage(it.bytes, Modifier.size(120.dp, 48.dp)) }
                                    TextButton(onClick = { scope.launch { repository.refreshCaptcha() } }, enabled = !state.busy) { Text("换一张") }
                                }
                                OutlinedTextField(code, { code = it.take(4) }, label = { Text("验证码（可修正识别结果）") }, singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = rememberLogin, onCheckedChange = { rememberLogin = it }, enabled = !state.busy)
                            Text("保持登录与自动重登", style = MaterialTheme.typography.bodyMedium)
                        }
                        Button(onClick = { scope.launch {
                            repository.signIn(account, password, code, rememberLogin, autoSync)
                            if (repository.uiState.value.signedIn) { password = ""; showLogin = false }
                        } }, enabled = !state.busy && account.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Text(if (code.isBlank()) "自动识别验证码并登录" else "登录并同步课表")
                        }
                        TextButton(onClick = { scope.launch { repository.refreshCaptcha() } }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("获取验证码 / 手动登录") }
                        Text("账号、密码与会话仅加密保存在本机。验证码离线识别，识别失败可手动填写。",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (state.signedIn) item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("账号偏好", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text("保持登录"); Text("会话失效时自动重新登录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Switch(state.remember, { scope.launch { repository.updatePreferences(it, state.autoSync) } }, enabled = !state.busy)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text("自动同步"); Text("打开应用时检查，后台定期更新", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Switch(state.autoSync, { autoSync = it; scope.launch { repository.updatePreferences(state.remember, it) } }, enabled = !state.busy)
                        }
                        TextButton(onClick = { showSignOut = true }, enabled = !state.busy) { Text("退出账号", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            item {
                Card(onClick = { onNavigate(Destination.NotificationSettings) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(vectorResource(Res.drawable.calendar_today_24px), null, Modifier.size(28.dp))
                        Column(Modifier.weight(1f)) {
                            Text("上课提醒与明日预告", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("课前提醒，前一晚安排好明天", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(vectorResource(Res.drawable.chevron_right_24px), null)
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("课表之外", style = MaterialTheme.typography.titleMedium)
                    Text("课程编辑、提醒、小组件、备份和其他学校导入，都在工具与设置中。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilledTonalButton(onClick = { onNavigate(Destination.Settings) }) {
                        Icon(vectorResource(Res.drawable.tune_24px), null); Spacer(Modifier.width(8.dp)); Text("工具与设置")
                    }
                    Text("节次时间可在“作息管理”中按学校安排校准。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    if (showSignOut) AlertDialog(onDismissRequest = { showSignOut = false }, title = { Text("退出教务账号？") },
        text = { Text("本地课表会保留。退出后将停止自动登录与教务同步。") },
        confirmButton = { TextButton({ showSignOut = false; scope.launch { repository.signOut() } }) { Text("退出账号") } },
        dismissButton = { TextButton({ showSignOut = false }) { Text("取消") } })
}
