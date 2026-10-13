// Modified by AconHol (2026): move the privacy statement into settings.
package com.xingheyuzhuan.shiguangschedule.ui.settings
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun LearningPrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("学习用途与隐私承诺") },
        text = { Text("本项目为个人开发学习与研究使用，非学校官方产品。请仅处理本人有权使用的数据，并遵守适用规则与必要授权。\n\n开发者绝不会私自盗取、收集、留存、出售或分享你的账号、密码、姓名、课表等个人信息。本应用没有开发者的数据收集服务器，不会将这些信息上传给开发者。账号与课表保存在你的设备；保持登录的信息在本机加密保存。\n\n你主动配置的备份、导出、分享或日历操作由你控制；系统备份由设备设置控制，登录凭据已排除在系统备份之外。联网服务仍受相应服务方的规则与隐私政策约束。\n\n学习用途声明不替代必要授权，也不免除依法应承担的责任。", modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } }
    )
}
