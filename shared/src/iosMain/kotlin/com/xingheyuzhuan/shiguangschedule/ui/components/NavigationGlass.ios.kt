// Modified by AconHol (2026): platform fallback for the glass navigation surface.
package com.xingheyuzhuan.shiguangschedule.ui.components
import androidx.compose.runtime.Composable
@Composable actual fun rememberNavigationGlass(): NavigationGlass = rememberFrostedNavigationGlass()
