// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import com.xingheyuzhuan.shiguangschedule.data.repository.*
import com.xingheyuzhuan.shiguangschedule.hbmu.*
import com.xingheyuzhuan.shiguangschedule.ui.theme.ShiguangScheduleTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.jetbrains.skia.EncodedImageFormat
import org.koin.core.context.GlobalContext
import org.koin.plugin.module.dsl.startKoin
import java.io.File

/** Headless layout QA for the shared Compose UI. No network requests or credentials are used. */
@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>): Unit = runBlocking {
    startKoin<DesktopAppConfig>()
    val koin = GlobalContext.get()
    val snapshot = System.getenv("HBMU_PREVIEW_SNAPSHOT")?.let { File(it) }
    if (snapshot != null) {
        val tables = koin.get<CourseTableRepository>()
        val table = tables.getAllCourseTables().first { it.isNotEmpty() }.first()
        val parsed = HbmuScheduleParser.parse(snapshot.readText())
        koin.get<CourseConversionRepository>().importCourseTableFromJson(table.id, parsed.model)
        val settings = koin.get<AppSettingsRepository>()
        settings.insertOrUpdateAppSettings(settings.getAppSettingsOnce().copy(currentCourseTableId = table.id))
        val hbmu = koin.get<HbmuRepository>()
        @Suppress("UNCHECKED_CAST")
        val state = HbmuRepository::class.java.getDeclaredField("state").apply { isAccessible = true }.get(hbmu) as MutableStateFlow<HbmuState>
        state.value = state.value.copy(account = "20260001", displayName = "湖医学子", signedIn = true, connected = true, semester = "202601", lastSync = System.currentTimeMillis(), recordCount = parsed.total,
            terms = listOf(HbmuTerm("202601", "2026—2027 学年 · 第一学期")), message = "已同步 ${parsed.total} 次排课，可离线查看")
    }
    val output = File(System.getenv("HBMU_PREVIEW_OUTPUT") ?: "build/hbmu-preview").apply { mkdirs() }
    withContext(Dispatchers.Main) {
        suspend fun settle(scene: ImageComposeScene) {
            repeat(8) { scene.render(System.nanoTime()).close(); delay(100) }
        }
        fun capture(scene: ImageComposeScene, name: String) {
            scene.render(System.nanoTime()).use { rendered -> rendered.encodeToData(EncodedImageFormat.PNG)?.use { data ->
                File(output, "$name.png").writeBytes(data.bytes)
            } }
        }
        fun verticalScroller(scene: ImageComposeScene): SemanticsNode {
            fun find(node: SemanticsNode): SemanticsNode? {
                // The pager also retains offscreen grids with empty clipped bounds.
                if (node.boundsInRoot.width > 0f && node.boundsInRoot.height > 0f &&
                    node.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.maxValue?.invoke()?.let { it > 0f } == true) return node
                return node.children.firstNotNullOfOrNull { find(it) }
            }
            return scene.semanticsOwners.firstNotNullOfOrNull { find(it.rootSemanticsNode) }
                ?: error("Timetable must expose a vertical scrolling action")
        }
        suspend fun scrollGrid(scene: ImageComposeScene, toBottom: Boolean) {
            val scroller = verticalScroller(scene)
            val range = checkNotNull(scroller.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange))
            val action = checkNotNull(scroller.config.getOrNull(SemanticsActions.ScrollBy)?.action)
            check(action(0f, if (toBottom) range.maxValue() else -range.maxValue()))
            settle(scene)
            if (kotlin.math.abs(range.value() - if (toBottom) range.maxValue() else 0f) >= 1f) settle(scene)
            check(kotlin.math.abs(range.value() - if (toBottom) range.maxValue() else 0f) < 1f) {
                "Grid scrolling stopped at ${range.value()} of ${range.maxValue()}, bottom=$toBottom, bounds=${scroller.boundsInRoot}"
            }
            println("Timetable QA: scroll=${range.value()}/${range.maxValue()}, bottom=$toBottom")
        }
        for ((name, destination) in listOf("today" to Destination.TodaySchedule, "week" to Destination.CourseSchedule, "academic" to Destination.Hbmu, "tools" to Destination.Settings,
            "contributors" to Destination.ContributionList, "theme" to Destination.ThemeSettings, "about" to Destination.MoreOptions)) {
            for (dark in listOf(false, true)) {
                val scene = ImageComposeScene(width = 720, height = 1600, density = Density(2f), coroutineContext = coroutineContext) {
                    ShiguangScheduleTheme(darkTheme = dark, dynamicColor = false) {
                        Surface(Modifier.fillMaxSize()) { AppNavigation(destination) }
                    }
                }
                repeat(8) { scene.render(System.nanoTime()).close(); delay(100) }
                scene.render(System.nanoTime()).use { image ->
                    image.encodeToData(EncodedImageFormat.PNG)?.use { data ->
                        File(output, "$name-${if (dark) "dark" else "light"}.png").writeBytes(data.bytes)
                    }
                }
                if (name == "week") {
                    val theme = if (dark) "dark" else "light"
                    scrollGrid(scene, toBottom = true)
                    capture(scene, "week-bottom-$theme")
                    scrollGrid(scene, toBottom = false)
                    scene.sendPointerEvent(PointerEventType.Press, Offset(88f, 184f))
                    scene.sendPointerEvent(PointerEventType.Release, Offset(88f, 184f))
                    settle(scene)
                    capture(scene, "week-previous-$theme")
                    scene.sendPointerEvent(PointerEventType.Press, Offset(632f, 184f))
                    scene.sendPointerEvent(PointerEventType.Release, Offset(632f, 184f))
                    settle(scene)
                    capture(scene, "week-returned-$theme")
                    scene.sendPointerEvent(PointerEventType.Press, Offset(88f, 184f))
                    scene.sendPointerEvent(PointerEventType.Release, Offset(88f, 184f))
                    settle(scene)
                    scene.sendPointerEvent(PointerEventType.Press, Offset(610f, 300f))
                    scene.sendPointerEvent(PointerEventType.Release, Offset(610f, 300f))
                    settle(scene)
                    capture(scene, "week-today-$theme")
                    scene.sendPointerEvent(PointerEventType.Press, Offset(360f, 184f))
                    scene.sendPointerEvent(PointerEventType.Release, Offset(360f, 184f))
                    settle(scene)
                    capture(scene, "week-selector-$theme")
                }
                scene.close()
            }
        }
        val settings = koin.get<AppSettingsRepository>()
        val tableId = settings.getAppSettingsOnce().currentCourseTableId
        val tableConfig = settings.getCourseConfigOnce(tableId)
        val styles = koin.get<StyleSettingsRepository>()
        val originalStyle = styles.getStyleOnce()
        try {
            if (tableConfig != null) settings.insertOrUpdateCourseConfig(tableConfig.copy(showWeekends = true))
            for (mode in listOf(ScheduleModeProto.SECTION_MODE, ScheduleModeProto.TIME_24H_MODE)) {
                styles.setScheduleMode(mode)
                for (dark in listOf(false, true)) {
                    val scene = ImageComposeScene(width = 720, height = 1600, density = Density(2f), coroutineContext = coroutineContext) {
                        ShiguangScheduleTheme(darkTheme = dark, dynamicColor = false) {
                            Surface(Modifier.fillMaxSize()) { AppNavigation(Destination.CourseSchedule) }
                        }
                    }
                    settle(scene)
                    capture(scene, "week-${if (mode == ScheduleModeProto.TIME_24H_MODE) "24h" else "seven"}-${if (dark) "dark" else "light"}")
                    scene.close()
                }
            }
        } finally {
            if (tableConfig != null) settings.insertOrUpdateCourseConfig(tableConfig)
            styles.setScheduleMode(originalStyle.scheduleMode)
        }
        // Exercise the actual navigation controls, including the reverse direction.
        val navigationScene = ImageComposeScene(width = 720, height = 1600, density = Density(2f), coroutineContext = coroutineContext) {
            ShiguangScheduleTheme(darkTheme = false, dynamicColor = false) {
                Surface(Modifier.fillMaxSize()) { AppNavigation(Destination.TodaySchedule) }
            }
        }
        repeat(8) { navigationScene.render(System.nanoTime()).close(); delay(100) }
        for ((name, x) in listOf("forward" to 360f, "backward" to 120f)) {
            navigationScene.sendPointerEvent(PointerEventType.Press, Offset(x, 1500f))
            navigationScene.sendPointerEvent(PointerEventType.Release, Offset(x, 1500f))
            val animationStart = System.nanoTime()
            repeat(28) { frame ->
                // PNG encoding must not advance the animation clock and skip intermediate frames.
                navigationScene.render(animationStart + frame * 20_000_000L).use { rendered -> rendered.encodeToData(EncodedImageFormat.PNG)?.use { data ->
                    File(output, "navigation-$name-${frame.toString().padStart(2, '0')}.png").writeBytes(data.bytes)
                } }
                delay(1)
            }
            repeat(4) { navigationScene.render(System.nanoTime()).close(); delay(100) }
        }
        navigationScene.close()
        val hbmu = koin.get<HbmuRepository>()
        @Suppress("UNCHECKED_CAST")
        val state = HbmuRepository::class.java.getDeclaredField("state").apply { isAccessible = true }.get(hbmu) as MutableStateFlow<HbmuState>
        state.value = HbmuState(semester = "202601")
        for (dark in listOf(false, true)) {
            val scene = ImageComposeScene(width = 720, height = 1600, density = Density(2f), coroutineContext = coroutineContext) {
                ShiguangScheduleTheme(darkTheme = dark, dynamicColor = false) {
                    Surface(Modifier.fillMaxSize()) { AppNavigation(Destination.Hbmu) }
                }
            }
            repeat(5) { scene.render(System.nanoTime()).close(); delay(100) }
            scene.sendPointerEvent(PointerEventType.Scroll, Offset(350f, 850f), scrollDelta = Offset(0f, 14f))
            repeat(8) { scene.render(System.nanoTime()).close(); delay(100) }
            scene.render(System.nanoTime()).use { image -> image.encodeToData(EncodedImageFormat.PNG)?.use { data ->
                File(output, "login-${if (dark) "dark" else "light"}.png").writeBytes(data.bytes)
            } }
            scene.close()
        }
    }
    GlobalContext.stopKoin()
    println("HBMU shared Compose preview rendered")
    kotlin.system.exitProcess(0)
}
