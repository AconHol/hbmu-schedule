// Modified by AconHol (2026): real lens refraction using Kyant0 Backdrop (Apache-2.0).
package com.xingheyuzhuan.shiguangschedule.ui.components

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

@Composable
actual fun rememberNavigationGlass(): NavigationGlass {
    if (Build.VERSION.SDK_INT < 33) return rememberFrostedNavigationGlass()
    val backdrop = rememberLayerBackdrop()
    return remember(backdrop) {
        object : NavigationGlass {
            override val source: Modifier get() = Modifier.layerBackdrop(backdrop)
            @Composable
            override fun surface(shape: Shape, tint: Color): Modifier = Modifier.drawBackdrop(
                backdrop = backdrop, shape = { shape },
                effects = {
                    vibrancy()
                    blur(20.dp.toPx())
                    lens(16.dp.toPx(), 24.dp.toPx())
                },
                onDrawSurface = { drawRect(tint.copy(alpha = .30f)) }
            )
        }
    }
}
