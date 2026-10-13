// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import dev.chrisbanes.haze.*
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.*

val FloatingNavigationHeight = 84.dp

private data class NavItemData(val label: String, val destination: Destination, val selectedIcon: ImageVector, val icon: ImageVector)

@Composable
fun AdaptiveNavigationScaffold(
    currentDestination: Destination,
    onTabSelected: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    showNavigation: Boolean = true,
    navHideFractionProvider: () -> Float = { 0f },
    isTransparent: Boolean = false,
    navigationModifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val items = listOf(
        NavItemData("今日", Destination.TodaySchedule, vectorResource(Res.drawable.view_agenda_filled_24px), vectorResource(Res.drawable.view_agenda_24px)),
        NavItemData("课表", Destination.CourseSchedule, vectorResource(Res.drawable.view_week_filled_24px), vectorResource(Res.drawable.view_week_24px)),
        NavItemData("我的", Destination.Hbmu, vectorResource(Res.drawable.account_circle_filled_24px), vectorResource(Res.drawable.account_circle_24px))
    )
    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    val haze = remember { HazeState() }
    val colors = MaterialTheme.colorScheme
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Box(modifier.fillMaxSize()) {
        if (layoutType == NavigationSuiteType.NavigationRail) {
            Row(Modifier.fillMaxSize()) {
                if (showNavigation) NavigationRail(containerColor = colors.surfaceContainerLow) {
                    items.forEach { item ->
                        val selected = currentDestination == item.destination
                        NavigationRailItem(selected = selected, onClick = { onTabSelected(item.destination) },
                            icon = { Icon(if (selected) item.selectedIcon else item.icon, null) }, label = { Text(item.label) })
                    }
                }
                Box(Modifier.weight(1f)) { content(PaddingValues(0.dp)) }
            }
        } else {
            Box(Modifier.fillMaxSize().hazeSource(haze)) { content(PaddingValues(0.dp)) }
            val density = LocalDensity.current
            val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val travel = with(density) { (FloatingNavigationHeight + bottomInset).toPx() }
            AnimatedVisibility(showNavigation && !keyboardOpen,
                enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)) {
                Box(Modifier.then(navigationModifier).graphicsLayer {
                    val fraction = navHideFractionProvider().coerceIn(0f, 1f)
                    translationY = travel * fraction
                    alpha = 1f - fraction
                }.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
                    val shape = RoundedCornerShape(32.dp)
                    Row(Modifier.widthIn(max = 360.dp).fillMaxWidth().height(64.dp)
                        .shadow(8.dp, shape, clip = false, ambientColor = colors.primary.copy(alpha = .1f), spotColor = colors.primary.copy(alpha = .1f))
                        .clip(shape).hazeEffect(haze, style = HazeStyle(
                            backgroundColor = colors.surface,
                            tint = HazeTint(colors.surfaceContainer.copy(alpha = if (isTransparent) .5f else .78f)),
                            blurRadius = 22.dp, noiseFactor = .02f,
                            fallbackTint = HazeTint(colors.surfaceContainer.copy(alpha = .97f))))
                        .border(1.dp, colors.outlineVariant.copy(alpha = .45f), shape)
                        .padding(5.dp).selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
                        items.forEach { item ->
                            val selected = currentDestination == item.destination
                            val background by animateColorAsState(if (selected) colors.secondaryContainer else Color.Transparent)
                            val foreground by animateColorAsState(if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant)
                            Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(27.dp)).background(background)
                                .selectable(selected, onClick = { if (!selected) onTabSelected(item.destination) }, role = Role.Tab),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                                Icon(if (selected) item.selectedIcon else item.icon, null, Modifier.size(22.dp), tint = foreground)
                                Text(item.label, style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = foreground)
                            }
                        }
                    }
                }
            }
        }
    }
}
