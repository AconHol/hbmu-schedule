// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDecoration
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalCourseNowOverride
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
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
    Thread.setDefaultUncaughtExceptionHandler { _, error -> error.printStackTrace(); kotlin.system.exitProcess(1) }
    startKoin<DesktopAppConfig>()
    val koin = GlobalContext.get()
    // Baseline screenshots exercise the full configured week; hiding has dedicated cases below.
    koin.get<StyleSettingsRepository>().setHideEmptyDays(false)
    val snapshot = System.getenv("HBMU_PREVIEW_SNAPSHOT")?.let { File(it) }
    if (snapshot != null) {
        val tables = koin.get<CourseTableRepository>()
        val table = tables.getAllCourseTables().first { it.isNotEmpty() }.first()
        val parsed = HbmuScheduleParser.parse(snapshot.readText())
        koin.get<CourseConversionRepository>().importCourseTableFromJson(table.id, HbmuDefaultTimes.withCalibratedBaseline(parsed.model))
        val settings = koin.get<AppSettingsRepository>()
        settings.insertOrUpdateAppSettings(settings.getAppSettingsOnce().copy(currentCourseTableId = table.id))
        val hbmu = koin.get<HbmuRepository>()
        @Suppress("UNCHECKED_CAST")
        val state = HbmuRepository::class.java.getDeclaredField("state").apply { isAccessible = true }.get(hbmu) as MutableStateFlow<HbmuState>
        state.value = state.value.copy(account = "20260001", displayName = "湖医学子", signedIn = true, connected = true, semester = "202601", lastSync = System.currentTimeMillis(), recordCount = parsed.total,
            terms = listOf(HbmuTerm("202601", "2026—2027 学年 · 第一学期")), message = "已同步 ${parsed.total} 次排课，可离线查看")
    }
    val previewDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val previewNow = LocalDateTime.parse("${previewDate}T12:00:00")
    @Composable fun PreviewPage(destination: Destination) {
        CompositionLocalProvider(LocalCourseNowOverride provides previewNow) {
            Surface(Modifier.fillMaxSize()) { AppNavigation(destination) }
        }
    }
    val output = File(System.getenv("HBMU_PREVIEW_OUTPUT") ?: "build/hbmu-preview").apply { mkdirs() }
    withContext(Dispatchers.Main) {
        val weekLayouts = mutableListOf<String>()
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
        fun actionNode(scene: ImageComposeScene, label: String): SemanticsNode {
            fun hasLabel(node: SemanticsNode): Boolean =
                node.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label) == true || node.children.any { hasLabel(it) }
            fun find(node: SemanticsNode): SemanticsNode? {
                if (node.boundsInRoot.width > 0f && node.boundsInRoot.height > 0f &&
                    node.config.getOrNull(SemanticsActions.OnClick)?.action != null && hasLabel(node)) return node
                return node.children.firstNotNullOfOrNull { find(it) }
            }
            return scene.semanticsOwners.firstNotNullOfOrNull { find(it.rootSemanticsNode) }
                ?: error("Visible action missing: $label")
        }
        fun click(scene: ImageComposeScene, label: String) {
            val position = actionNode(scene, label).boundsInRoot.center
            scene.sendPointerEvent(PointerEventType.Press, position)
            scene.sendPointerEvent(PointerEventType.Release, position)
        }
        fun recordWeekLayout(scene: ImageComposeScene, name: String) {
            val bodyTop = verticalScroller(scene).boundsInRoot.top
            for (label in listOf("上一周", "下一周", "选择周次", "回到本周", "切换课表", "课表设置")) {
                val bounds = actionNode(scene, label).boundsInRoot
                weekLayouts.add("$name,$bodyTop,$label,${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}")
            }
        }
        for ((name, destination) in listOf("today" to Destination.TodaySchedule, "week" to Destination.CourseSchedule, "academic" to Destination.Hbmu, "tools" to Destination.Settings,
            "contributors" to Destination.ContributionList, "theme" to Destination.ThemeSettings, "about" to Destination.MoreOptions,
            "notification" to Destination.NotificationSettings, "language" to Destination.LanguageSettings,
            "backup" to Destination.BackupAndRestore, "tables" to Destination.ManageCourseTables,
            "times" to Destination.TimeScheduleManagement, "courses" to Destination.CourseManagementList,
            "quick" to Destination.QuickActions, "holiday" to Destination.HolidayManagement)) {
            for (dark in listOf(false, true)) {
                val scene = ImageComposeScene(width = 720, height = 1600, density = Density(2f), coroutineContext = coroutineContext) {
                    ShiguangScheduleTheme(darkTheme = dark, dynamicColor = false) {
                        PreviewPage(destination)
                    }
                }
                repeat(8) { scene.render(System.nanoTime()).close(); delay(100) }
                scene.render(System.nanoTime()).use { image ->
                    image.encodeToData(EncodedImageFormat.PNG)?.use { data ->
                        File(output, "$name-${if (dark) "dark" else "light"}.png").writeBytes(data.bytes)
                    }
                }
                if (name == "week") {
                    fun texts(node: SemanticsNode): List<String> =
                        node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } + node.children.flatMap { texts(it) }
                    val renderedTexts = scene.semanticsOwners.flatMap { texts(it.rootSemanticsNode) }
                    check("08:40" in renderedTexts && "14:30" in renderedTexts) { "Calibrated local times must reach the visible grid" }
                    val theme = if (dark) "dark" else "light"
                    recordWeekLayout(scene, "week-$theme")
                    val initialBodyTop = verticalScroller(scene).boundsInRoot.top
                    scrollGrid(scene, toBottom = true)
                    capture(scene, "week-bottom-$theme")
                    scrollGrid(scene, toBottom = false)
                    click(scene, "上一周")
                    settle(scene)
                    check(verticalScroller(scene).boundsInRoot.top == initialBodyTop) { "Changing weeks must not shift the timetable" }
                    recordWeekLayout(scene, "week-previous-$theme")
                    capture(scene, "week-previous-$theme")
                    click(scene, "下一周")
                    settle(scene)
                    capture(scene, "week-returned-$theme")
                    click(scene, "上一周")
                    settle(scene)
                    click(scene, "回到本周")
                    settle(scene)
                    capture(scene, "week-today-$theme")
                    click(scene, "选择周次")
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
                            PreviewPage(Destination.CourseSchedule)
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
        val largeTextScene = ImageComposeScene(width = 720, height = 1600, density = Density(2f, fontScale = 1.6f), coroutineContext = coroutineContext) {
            ShiguangScheduleTheme(darkTheme = false, dynamicColor = false) {
                PreviewPage(Destination.CourseSchedule)
            }
        }
        settle(largeTextScene)
        recordWeekLayout(largeTextScene, "week-large-text-light")
        click(largeTextScene, "上一周")
        settle(largeTextScene)
        click(largeTextScene, "回到本周")
        settle(largeTextScene)
        capture(largeTextScene, "week-large-text-light")
        largeTextScene.close()
        if (snapshot != null) {
            val parsed = HbmuScheduleParser.parse(snapshot.readText())
            val calibratedModel = HbmuDefaultTimes.withCalibratedBaseline(parsed.model)
            val sparse = calibratedModel.copy(courses = calibratedModel.courses.filter { it.day in setOf(1, 3, 5) })
            val conversion = koin.get<CourseConversionRepository>()
            fun visibleDateCount(scene: ImageComposeScene): Int {
                fun count(node: SemanticsNode): Int =
                    (if (node.boundsInRoot.width > 0f && node.boundsInRoot.height > 0f &&
                        node.config.getOrNull(SemanticsProperties.ContentDescription)?.any { it.startsWith("课表日期：") } == true) 1 else 0) + node.children.sumOf { count(it) }
                return scene.semanticsOwners.sumOf { count(it.rootSemanticsNode) }
            }
            try {
                conversion.importCourseTableFromJson(tableId, sparse)
                styles.setHideEmptyDays(true)
                for (dark in listOf(false, true)) {
                    val theme = if (dark) "dark" else "light"
                    val scene = ImageComposeScene(width = 720, height = 1600, density = Density(2f), coroutineContext = coroutineContext) {
                        ShiguangScheduleTheme(darkTheme = dark, dynamicColor = false) {
                            PreviewPage(Destination.CourseSchedule)
                        }
                    }
                    settle(scene)
                    check(visibleDateCount(scene) == 3) { "Sparse week must display exactly three dates" }
                    capture(scene, "week-hidden-$theme")
                    fun findLongClick(node: SemanticsNode): SemanticsNode? {
                        if (node.boundsInRoot.width > 0f && node.boundsInRoot.height > 0f &&
                            node.config.getOrNull(SemanticsActions.OnLongClick)?.action != null) return node
                        return node.children.firstNotNullOfOrNull { findLongClick(it) }
                    }
                    val editable = scene.semanticsOwners.firstNotNullOfOrNull { findLongClick(it.rootSemanticsNode) }
                        ?: error("Visible course must remain editable with hidden days")
                    check(editable.config.getOrNull(SemanticsActions.OnLongClick)?.action?.invoke() == true)
                    settle(scene)
                    check(visibleDateCount(scene) == 5) { "Editing must reveal empty days as move targets" }
                    capture(scene, "week-editing-$theme")
                    click(scene, "上一周")
                    settle(scene)
                    check(visibleDateCount(scene) == 0) { "Empty week must hide all date columns" }
                    capture(scene, "week-empty-$theme")
                    click(scene, "显示完整一周")
                    settle(scene)
                    check(visibleDateCount(scene) == 5) { "Empty week must allow restoring the grid for new courses" }
                    capture(scene, "week-empty-restored-$theme")
                    scene.close()
                }
                println("Timetable QA: hidden days, editing targets, empty week, restoration passed in both themes")
            } finally {
                conversion.importCourseTableFromJson(tableId, calibratedModel)
                styles.setHideEmptyDays(false)
            }
        }
        File(output, "week-layout-check.csv").writeText("name,body_top,control,left,top,right,bottom\n" + weekLayouts.joinToString("\n"))
        // Exercise actual title-only decoration and the persistent dismissible footer hint.
        fun taggedNodes(scene: ImageComposeScene, tag: String): List<SemanticsNode> {
            fun find(node: SemanticsNode): List<SemanticsNode> =
                (if (node.boundsInRoot.width > 0f && node.boundsInRoot.height > 0f && node.config.getOrNull(SemanticsProperties.TestTag) == tag) listOf(node) else emptyList()) + node.children.flatMap { find(it) }
            return scene.semanticsOwners.flatMap { find(it.unmergedRootSemanticsNode) }
        }
        fun decoration(node: SemanticsNode): TextDecoration? {
            val results = mutableListOf<TextLayoutResult>()
            check(node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results) == true)
            return results.first().layoutInput.style.textDecoration
        }
        styles.setStrikeCompletedToday(true)
        styles.setHideReminderTip(false)
        val statusScene = ImageComposeScene(width = 720, height = 1600, density = Density(2f), coroutineContext = coroutineContext) {
            ShiguangScheduleTheme(darkTheme = false, dynamicColor = false) { PreviewPage(Destination.TodaySchedule) }
        }
        settle(statusScene)
        println("Temporal QA: visible titles=${taggedNodes(statusScene, "today-course-title").size}, decorations=${taggedNodes(statusScene, "today-course-title").map { decoration(it) }}")
        check(taggedNodes(statusScene, "today-course-title").any {
            it.config.getOrNull(SemanticsProperties.StateDescription) == "已结束，名称已划线"
        }) { "Completed course name must be struck through" }
        check(taggedNodes(statusScene, "today-course-details").all { decoration(it) != TextDecoration.LineThrough }) { "Classroom and teacher must not be struck through" }
        styles.setStrikeCompletedToday(false)
        settle(statusScene)
        check(taggedNodes(statusScene, "today-course-title").all {
            it.config.getOrNull(SemanticsProperties.StateDescription) != "已结束，名称已划线"
        })
        styles.setStrikeCompletedToday(true)
        val scroller = verticalScroller(statusScene)
        check(scroller.config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(0f, 100000f) == true)
        settle(statusScene)
        capture(statusScene, "today-footer-light")
        click(statusScene, "关闭提醒设置提示")
        settle(statusScene)
        check(styles.getStyleOnce().hideReminderTip) { "Closing the hint must persist the hidden state" }
        capture(statusScene, "today-footer-dismissed-light")
        statusScene.close()
        styles.setHideReminderTip(false)
        println("Temporal QA: name-only strike, independent toggle, persistent footer dismissal passed")
        // Exercise the actual navigation controls, including the reverse direction.
        val navigationScene = ImageComposeScene(width = 720, height = 1600, density = Density(2f), coroutineContext = coroutineContext) {
            ShiguangScheduleTheme(darkTheme = false, dynamicColor = false) {
                PreviewPage(Destination.TodaySchedule)
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
                    PreviewPage(Destination.Hbmu)
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
