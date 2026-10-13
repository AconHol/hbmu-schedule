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
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
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
        for ((name, destination) in listOf("today" to Destination.TodaySchedule, "week" to Destination.CourseSchedule, "academic" to Destination.Hbmu, "tools" to Destination.Settings,
            "contributors" to Destination.ContributionList)) {
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
                scene.close()
            }
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
