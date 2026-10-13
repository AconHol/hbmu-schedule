package com.xingheyuzhuan.shiguangschedule.hbmu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
fun HbmuBrandHeader(title: String, subtitle: String, onTools: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("HBMU / 湖北医药学院", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalIconButton(onClick = onTools, modifier = Modifier.size(48.dp)) {
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
    var semesterInput by remember(state.semester) { mutableStateOf(state.semester) }
    LaunchedEffect(state.captcha) { code = state.captcha?.recognized.orEmpty() }

    Scaffold { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 112.dp)
        ) {
            item { HbmuBrandHeader("教务连接", "连接一次，课表随行", { onNavigate(Destination.Settings) }) }
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary) {
                                Icon(vectorResource(Res.drawable.school_24px), null, Modifier.padding(12.dp).size(28.dp), tint = MaterialTheme.colorScheme.onPrimary)
                            }
                            Column {
                                Text("湖北医药学院", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("教务系统 · jw.hbmu.edu.cn", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(if (state.connected) "课表已连接" else if (state.lastSync > 0) "本地课表已保存" else "让每一节课，都井井有条", style = MaterialTheme.typography.headlineSmall)
                        Text(state.message, style = MaterialTheme.typography.bodyMedium)
                        if (state.lastSync > 0) {
                            val date = Instant.fromEpochMilliseconds(state.lastSync).toLocalDateTime(TimeZone.currentSystemDefault())
                            Text("上次同步 ${date.date} ${date.hour.toString().padStart(2, '0')}:${date.minute.toString().padStart(2, '0')} · ${state.recordCount} 次排课",
                                style = MaterialTheme.typography.labelMedium)
                        }
                        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }
            if (state.error != null) item {
                Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer) {
                    Text(state.error!!, Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
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
                                        termMenu = false; scope.launch { repository.selectSemester(term.code) }
                                    }) }
                                }
                            }
                        } else {
                            OutlinedTextField(value = semesterInput, onValueChange = { value ->
                                semesterInput = value.filter(Char::isDigit).take(6)
                                if (semesterInput.length == 6) scope.launch { repository.selectSemester(semesterInput) }
                            }, label = { Text("学期代码") }, supportingText = { Text("登录后自动读取学校学期；例如 202601") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                                enabled = !state.busy, modifier = Modifier.fillMaxWidth())
                        }
                        Button(onClick = { scope.launch { repository.sync() } }, enabled = !state.busy && state.account.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Icon(vectorResource(Res.drawable.refresh_24px), null); Spacer(Modifier.width(8.dp)); Text("同步整学期课表")
                        }
                    }
                }
            }
            if (!state.connected) item {
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
                            if (repository.uiState.value.connected) password = ""
                        } }, enabled = !state.busy && account.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Text(if (code.isBlank()) "自动识别验证码并登录" else "登录并同步课表")
                        }
                        TextButton(onClick = { scope.launch { repository.refreshCaptcha() } }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("获取验证码 / 手动登录") }
                        Text("账号、密码与会话仅加密保存在本机。验证码离线识别，识别失败可手动填写。",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (state.connected) item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("登录状态", style = MaterialTheme.typography.titleMedium)
                        Text("学号 ${state.account}", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text("保持登录"); Text("会话失效时自动重新登录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Switch(state.remember, { scope.launch { repository.updatePreferences(it, state.autoSync) } }, enabled = !state.busy)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text("自动同步"); Text("打开应用时检查，后台定期更新", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Switch(state.autoSync, { autoSync = it; scope.launch { repository.updatePreferences(state.remember, it) } }, enabled = !state.busy)
                        }
                        TextButton(onClick = { scope.launch { repository.signOut() } }, enabled = !state.busy) { Text("退出登录", color = MaterialTheme.colorScheme.error) }
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
}
