// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.xingheyuzhuan.shiguangschedule.Destination
import dev.chrisbanes.haze.*
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.*

val FloatingNavigationHeight = 96.dp
val LocalNavigationContentBottom = staticCompositionLocalOf<Dp> { 0.dp }

fun mainTabIndex(destination: Destination): Int = when (destination) {
    Destination.TodaySchedule -> 0
    Destination.CourseSchedule -> 1
    Destination.Hbmu -> 2
    else -> -1
}

fun mainTabDirection(from: Destination, to: Destination): Int {
    val first = mainTabIndex(from)
    val second = mainTabIndex(to)
    return mainTabDirection(first, second)
}

fun mainTabDirection(first: Int, second: Int): Int {
    return if (first < 0 || second < 0) 0 else (second - first).compareTo(0)
}

private data class NavItemData(val label: String, val destination: Destination, val selectedIcon: ImageVector, val icon: ImageVector)

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
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
    val glass = rememberNavigationGlass()
    val colors = MaterialTheme.colorScheme
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var lastRootIndex by remember { mutableIntStateOf(mainTabIndex(currentDestination).coerceAtLeast(0)) }
    LaunchedEffect(currentDestination) { mainTabIndex(currentDestination).takeIf { it >= 0 }?.let { lastRootIndex = it } }
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
            val density = LocalDensity.current
            val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val contentBottom = if (showNavigation && !keyboardOpen) FloatingNavigationHeight + bottomInset + 12.dp else 0.dp
            CompositionLocalProvider(LocalNavigationContentBottom provides contentBottom) {
                Box(Modifier.fillMaxSize().then(glass.source)) { content(PaddingValues(0.dp)) }
            }
            val travel = with(density) { (FloatingNavigationHeight + bottomInset).toPx() }
            AnimatedVisibility(showNavigation && !keyboardOpen,
                enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)) {
                Box(Modifier.then(navigationModifier).graphicsLayer {
                    val fraction = navHideFractionProvider().coerceIn(0f, 1f)
                    translationY = travel * fraction
                    alpha = 1f - fraction
                }.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
                    val shape = CircleShape
                    BoxWithConstraints(Modifier.widthIn(max = 400.dp).fillMaxWidth().height(76.dp)
                        .shadow(12.dp, shape, clip = false, ambientColor = colors.onSurface.copy(alpha = .06f), spotColor = colors.onSurface.copy(alpha = .10f))
                        .then(glass.surface(shape, colors.surfaceContainer))
                        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .65f), colors.outlineVariant.copy(alpha = .12f), Color.White.copy(alpha = .30f))), shape)
                        .clip(shape)
                        .padding(6.dp)) {
                        val tabWidth = maxWidth / items.size
                        val selectedIndex = mainTabIndex(currentDestination).takeIf { it >= 0 } ?: lastRootIndex
                        var dragPosition by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
                        var isDragging by remember { mutableStateOf(false) }
                        val capsuleOffset by animateDpAsState(tabWidth * if (isDragging) dragPosition else selectedIndex.toFloat(),
                            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(), label = "navigationCapsule")
                        Box(Modifier.offset(x = capsuleOffset).width(tabWidth).fillMaxHeight()
                            .clip(CircleShape).background(colors.secondaryContainer.copy(alpha = .86f))
                            .border(1.dp, colors.onSecondaryContainer.copy(alpha = .08f), CircleShape))
                        CompositionLocalProvider(LocalRippleConfiguration provides null) {
                        NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp,
                            windowInsets = WindowInsets(0, 0, 0, 0), modifier = Modifier.fillMaxSize()
                                .pointerInput(tabWidth, selectedIndex) {
                                    val widthPx = tabWidth.toPx()
                                    detectHorizontalDragGestures(
                                        onDragStart = { isDragging = true; dragPosition = selectedIndex.toFloat() },
                                        onDragCancel = { isDragging = false },
                                        onDragEnd = {
                                            isDragging = false
                                            val target = dragPosition.roundToInt().coerceIn(0, items.lastIndex)
                                            if (target != selectedIndex) onTabSelected(items[target].destination)
                                        },
                                        onHorizontalDrag = { change, delta ->
                                            change.consume()
                                            dragPosition = (dragPosition + delta / widthPx).coerceIn(0f, items.lastIndex.toFloat())
                                        }
                                    )
                                }) {
                            items.forEachIndexed { index, item ->
                                val selected = selectedIndex == index
                                NavigationBarItem(selected = selected,
                                    onClick = { if (!selected) onTabSelected(item.destination) },
                                    icon = { Icon(if (selected) item.selectedIcon else item.icon, null, Modifier.size(24.dp)) },
                                    label = { Text(item.label, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                                    alwaysShowLabel = true,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = colors.onSecondaryContainer,
                                        selectedTextColor = colors.onSecondaryContainer,
                                        unselectedIconColor = colors.onSurface,
                                        unselectedTextColor = colors.onSurfaceVariant,
                                        indicatorColor = Color.Transparent
                                    ))
                            }
                        }
                        }
                    }
                }
            }
        }
    }
}
