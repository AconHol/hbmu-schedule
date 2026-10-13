// Modified by AconHol (2026): adaptive glass navigation surface.
package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.*

interface NavigationGlass {
    val source: Modifier
    @Composable fun surface(shape: Shape, tint: Color): Modifier
}

@Composable
expect fun rememberNavigationGlass(): NavigationGlass

@Composable
fun rememberFrostedNavigationGlass(): NavigationGlass {
    val state = remember { HazeState() }
    return remember(state) {
        object : NavigationGlass {
            override val source: Modifier get() = Modifier.hazeSource(state)
            @Composable
            override fun surface(shape: Shape, tint: Color): Modifier = Modifier.clip(shape).hazeEffect(
                state, style = HazeStyle(
                    backgroundColor = tint,
                    tint = HazeTint(tint.copy(alpha = .24f)),
                    blurRadius = 36.dp, noiseFactor = .025f,
                    fallbackTint = HazeTint(tint.copy(alpha = .95f))
                )
            )
        }
    }
}
