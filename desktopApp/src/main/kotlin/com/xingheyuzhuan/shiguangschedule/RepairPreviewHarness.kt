package com.xingheyuzhuan.shiguangschedule

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import com.xingheyuzhuan.shiguangschedule.data.repository.*
import com.xingheyuzhuan.shiguangschedule.hbmu.*
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalCourseNowOverride
import com.xingheyuzhuan.shiguangschedule.ui.theme.ShiguangScheduleTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime
import org.jetbrains.skia.EncodedImageFormat
import org.koin.core.context.GlobalContext
import org.koin.plugin.module.dsl.startKoin
import java.io.File

/** Small screenshot renderer, without the full layout QA or unit-test suite. */
@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>): Unit = runBlocking {
    Thread.setDefaultUncaughtExceptionHandler { _, error -> error.printStackTrace(); kotlin.system.exitProcess(1) }
    startKoin<DesktopAppConfig>()
    val koin = GlobalContext.get()
    val table = koin.get<CourseTableRepository>().getAllCourseTables().first { it.isNotEmpty() }.first()
    val fixture = File(checkNotNull(System.getenv("HBMU_PREVIEW_SNAPSHOT")))
    val parsed = HbmuScheduleParser.parse(fixture.readText())
    koin.get<CourseConversionRepository>().importCourseTableFromJson(table.id, HbmuDefaultTimes.withCalibratedBaseline(parsed.model))
    val appSettings = koin.get<AppSettingsRepository>()
    appSettings.insertOrUpdateAppSettings(appSettings.getAppSettingsOnce().copy(currentCourseTableId = table.id))
    val styles = koin.get<StyleSettingsRepository>()
    val originalStyle = styles.getStyleOnce()
    val output = File(checkNotNull(System.getenv("HBMU_PREVIEW_OUTPUT"))).apply { mkdirs() }
    val clock = LocalDateTime.parse("2026-10-14T15:00:00")
    suspend fun settle(scene: ImageComposeScene) {
        repeat(8) { scene.render(System.nanoTime()).close(); delay(100) }
    }
    fun capture(scene: ImageComposeScene, name: String) {
        scene.render(System.nanoTime()).use { image -> image.encodeToData(EncodedImageFormat.PNG)?.use {
            File(output, "$name.png").writeBytes(it.bytes)
        } }
    }
    suspend fun scroll(scene: ImageComposeScene, amount: Float) {
        fun find(node: SemanticsNode): SemanticsNode? {
            if (node.boundsInRoot.width > 0f && node.boundsInRoot.height > 0f &&
                node.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.maxValue?.invoke()?.let { it > 0f } == true) return node
            return node.children.firstNotNullOfOrNull { find(it) }
        }
        scene.semanticsOwners.firstNotNullOfOrNull { find(it.rootSemanticsNode) }
            ?.config?.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(0f, amount)
        settle(scene)
    }
    try {
        styles.setHideEmptyDays(false)
        styles.setStrikeCompletedToday(true)
        styles.setDimCompletedToday(true)
        withContext(Dispatchers.Main) {
            for (dark in listOf(false, true)) {
                val theme = if (dark) "dark" else "light"
                for ((name, destination) in listOf("today" to Destination.TodaySchedule,
                    "week" to Destination.CourseSchedule, "settings" to Destination.Settings,
                    "style" to Destination.StyleSettings)) {
                    val scene = ImageComposeScene(720, 1600, Density(2f), coroutineContext = coroutineContext) {
                        ShiguangScheduleTheme(darkTheme = dark, dynamicColor = false) {
                            CompositionLocalProvider(LocalCourseNowOverride provides clock) {
                                Surface(Modifier.fillMaxSize()) { AppNavigation(destination) }
                            }
                        }
                    }
                    settle(scene)
                    capture(scene, "$name-$theme")
                    if (name == "week" || name == "today" || name == "style") {
                        scroll(scene, if (name == "week") 450f else if (name == "style") 980f else 330f)
                        capture(scene, "$name-scrolled-$theme")
                    }
                    if (name == "today" && !dark) {
                        styles.setStrikeCompletedToday(false)
                        styles.setDimCompletedToday(false)
                        settle(scene)
                        capture(scene, "today-effects-off-light")
                        styles.setStrikeCompletedToday(true)
                        styles.setDimCompletedToday(true)
                    }
                    scene.close()
                }
            }
        }
    } finally {
        styles.setHideEmptyDays(originalStyle.hideEmptyDays)
        styles.setStrikeCompletedToday(originalStyle.strikeCompletedToday)
        styles.setDimCompletedToday(originalStyle.dimCompletedToday)
        GlobalContext.stopKoin()
    }
    println("Repair screenshots rendered with fictional courses")
    kotlin.system.exitProcess(0)
}
