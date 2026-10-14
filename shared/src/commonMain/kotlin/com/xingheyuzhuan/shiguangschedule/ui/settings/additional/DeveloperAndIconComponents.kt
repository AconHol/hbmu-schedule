package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_app_icon
import shiguangschedule.shared.generated.resources.developer_mode_24px
import shiguangschedule.shared.generated.resources.hbmu_app_mark
import shiguangschedule.shared.generated.resources.item_developer_options

/**
 * 动态 App 图标头部组件（包含连续 5 次点击解锁开发者模式逻辑）
 */
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun DynamicAppIconHeader(
    isDeveloperModeEnabled: Boolean,
    onTriggerDeveloperMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var clickCount by remember { mutableIntStateOf(0) }

    // 背景色过渡动画
    val colors = MaterialTheme.colorScheme
    val targetColor = if (isDeveloperModeEnabled) colors.tertiaryContainer else colors.primaryContainer
    val animatedBgColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    val foreground by animateColorAsState(
        targetValue = if (isDeveloperModeEnabled) colors.onTertiaryContainer else colors.onPrimaryContainer,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )

    Box(
        modifier = modifier
            .size(120.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(animatedBgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                clickCount++
                if (clickCount >= 5) {
                    clickCount = 0
                    onTriggerDeveloperMode()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.hbmu_app_mark),
            contentDescription = stringResource(Res.string.a11y_app_icon),
            modifier = Modifier.fillMaxSize(),
            tint = foreground
        )
    }
}

/**
 * 开发者模式列表选项组件（带展开/收起动画）
 */
@Composable
fun DeveloperModeSettingItem(
    isDeveloperModeEnabled: Boolean,
    onDeveloperModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isDeveloperModeEnabled,
        enter = fadeIn(tween(300)) + expandVertically(tween(300)),
        exit = fadeOut(tween(300)) + shrinkVertically(tween(300)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingListItem(
                icon = vectorResource(Res.drawable.developer_mode_24px),
                title = stringResource(Res.string.item_developer_options),
                onClick = { onDeveloperModeChanged(!isDeveloperModeEnabled) },
                showDivider = false,
                trailingContent = {
                    Switch(
                        checked = isDeveloperModeEnabled,
                        onCheckedChange = { onDeveloperModeChanged(it) }
                    )
                }
            )
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                thickness = 1.dp
            )
        }
    }
}
