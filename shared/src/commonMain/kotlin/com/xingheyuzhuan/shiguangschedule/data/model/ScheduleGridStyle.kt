// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
package com.xingheyuzhuan.shiguangschedule.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.BorderTypeProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.DualColorProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleGridStyleProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.WidgetStyleProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.WidgetThemeModeProto

// 1. Compose 业务模型

/**
 * 浅色和深色模式下的颜色对。
 */
data class DualColor(val light: Color, val dark: Color)

/**
 * 小组件独立外观样式配置的业务模型
 */
data class WidgetStyle(
    val fontScale: Float = DEFAULT_FONT_SCALE,
    val backgroundAlpha: Float = DEFAULT_BACKGROUND_ALPHA,
    val hideTeacher: Boolean = false,
    val hideLocation: Boolean = false,
    val hideDate: Boolean = false,
    val themeMode: WidgetThemeModeProto = WidgetThemeModeProto.WIDGET_THEME_FOLLOW_SYSTEM,
    val seedColor: Long? = null
) {
    companion object {
        val DEFAULT_FONT_SCALE = 1.0f
        val DEFAULT_BACKGROUND_ALPHA = 1.0f

        val DEFAULT = WidgetStyle(
            fontScale = DEFAULT_FONT_SCALE,
            backgroundAlpha = DEFAULT_BACKGROUND_ALPHA,
            hideTeacher = false,
            hideLocation = false,
            hideDate = false,
            themeMode = WidgetThemeModeProto.WIDGET_THEME_FOLLOW_SYSTEM
        )
    }
}

/**
 * 课表网格样式配置的业务模型
 * 所有尺寸（Dp）属性使用 Float，颜色（Color）属性使用 Long。
 */
data class ScheduleGridStyle(
    // Grid 尺寸 (单位: Float/Dp)
    val timeColumnWidthDp: Float = DEFAULT_TIME_COLUMN_WIDTH,
    val dayHeaderHeightDp: Float = DEFAULT_DAY_HEADER_HEIGHT,
    val sectionHeightDp: Float = DEFAULT_SECTION_HEIGHT,

    // CourseBlock 外观 (单位: Float/Dp & Float)
    val courseBlockCornerRadiusDp: Float = DEFAULT_BLOCK_CORNER_RADIUS,
    val courseBlockOuterPaddingDp: Float = DEFAULT_BLOCK_OUTER_PADDING,
    val courseBlockInnerPaddingDp: Float = DEFAULT_BLOCK_INNER_PADDING,
    val courseBlockAlphaFloat: Float = DEFAULT_BLOCK_ALPHA,

    // 颜色列表
    val courseColorMaps: List<DualColor> = DEFAULT_COLOR_MAPS,

    val courseBlockFontScale: Float = DEFAULT_FONT_SCALE,

    // 界面开关与布局控制
    val hideGridLines: Boolean = true,
    val hideEmptyDays: Boolean = true,
    val dimCompletedText: Boolean = true,
    val dimCompletedBackground: Boolean = true,
    val highlightOngoing: Boolean = true,
    val strikeCompletedToday: Boolean = true,
    val hideReminderTip: Boolean = false,
    val hideSectionTime: Boolean = false,
    val hideDateUnderDay: Boolean = false,
    val showStartTime: Boolean = false,
    val hideLocation: Boolean = false,
    val hideTeacher: Boolean = false,
    val removeLocationAt: Boolean = false,
    val textAlignCenterHorizontal: Boolean = false,
    val textAlignCenterVertical: Boolean = false,
    val borderType: BorderTypeProto = BorderTypeProto.BORDER_TYPE_NONE,
    val scheduleMode: ScheduleModeProto = ScheduleModeProto.SECTION_MODE,
    val pageTextColorLong: Long? = null,
    val courseTextColorLong: Long? = null,

    // 小组件独立外观样式配置
    val widgetStyle: WidgetStyle = WidgetStyle.DEFAULT
) {

    fun generateRandomColorIndex(): Int {
        if (courseColorMaps.isEmpty()) return 0
        return kotlin.random.Random.nextInt(courseColorMaps.size)
    }

    companion object {
        // --- 默认常量
        val DEFAULT_TIME_COLUMN_WIDTH = 40f
        val DEFAULT_DAY_HEADER_HEIGHT = 56f
        val DEFAULT_SECTION_HEIGHT = 76f
        val DEFAULT_BLOCK_CORNER_RADIUS = 16f
        val DEFAULT_BLOCK_OUTER_PADDING = 2f
        val DEFAULT_BLOCK_INNER_PADDING = 6f
        val DEFAULT_BLOCK_ALPHA = 1f
        val DEFAULT_FONT_SCALE = 1f

        val DEFAULT_COLOR_MAPS = listOf(
            DualColor(light = Color(0xFFDCEBCB), dark = Color(0xFF354A27)),
            DualColor(light = Color(0xFFFFE8B9), dark = Color(0xFF58431F)),
            DualColor(light = Color(0xFFE5DDF5), dark = Color(0xFF47365F)),
            DualColor(light = Color(0xFFCDEAE3), dark = Color(0xFF254D44)),
            DualColor(light = Color(0xFFF5DCE1), dark = Color(0xFF613B45)),
            DualColor(light = Color(0xFFDDE7F6), dark = Color(0xFF32475F)),
            DualColor(light = Color(0xFFE9EBCB), dark = Color(0xFF484C2A)),
            DualColor(light = Color(0xFFD6EEF0), dark = Color(0xFF284E53)),
            DualColor(light = Color(0xFFF3DFCD), dark = Color(0xFF583F2D)),
            DualColor(light = Color(0xFFE8DCF0), dark = Color(0xFF523960)),
            DualColor(light = Color(0xFFD8E6D9), dark = Color(0xFF324D38)),
            DualColor(light = Color(0xFFEADFD4), dark = Color(0xFF504436)),
        )

        /**
         * 默认样式对象，用于首次启动或重置样式。
         */
        val DEFAULT = ScheduleGridStyle(
            timeColumnWidthDp = DEFAULT_TIME_COLUMN_WIDTH,
            dayHeaderHeightDp = DEFAULT_DAY_HEADER_HEIGHT,
            sectionHeightDp = DEFAULT_SECTION_HEIGHT,
            courseBlockCornerRadiusDp = DEFAULT_BLOCK_CORNER_RADIUS,
            courseBlockOuterPaddingDp = DEFAULT_BLOCK_OUTER_PADDING,
            courseBlockInnerPaddingDp = DEFAULT_BLOCK_INNER_PADDING,
            courseBlockAlphaFloat = DEFAULT_BLOCK_ALPHA,
            courseColorMaps = DEFAULT_COLOR_MAPS,
            courseBlockFontScale = DEFAULT_FONT_SCALE,
            hideGridLines = false,
            hideSectionTime = false,
            hideDateUnderDay = false,
            showStartTime = false,
            hideLocation = false,
            hideTeacher = false,
            removeLocationAt = false,
            textAlignCenterHorizontal = false,
            textAlignCenterVertical = false,
            borderType = BorderTypeProto.BORDER_TYPE_NONE,
            scheduleMode = ScheduleModeProto.SECTION_MODE,
            pageTextColorLong = null,
            courseTextColorLong = null,
            widgetStyle = WidgetStyle.DEFAULT
        )
    }
}


// 2. Proto ⇔ Compose 转换扩展函数

fun DualColorProto.toCompose(): DualColor {
    return DualColor(
        light = Color(this.light_color),
        dark = Color(this.dark_color)
    )
}

fun DualColor.toProto(): DualColorProto {
    return DualColorProto(
        light_color = this.light.toArgb().toLong(),
        dark_color = this.dark.toArgb().toLong()
    )
}

fun WidgetStyleProto.toCompose(): WidgetStyle {
    val d = WidgetStyle.DEFAULT

    return WidgetStyle(
        fontScale = this.font_scale ?: d.fontScale,
        backgroundAlpha = this.background_alpha ?: d.backgroundAlpha,
        hideTeacher = this.hide_teacher ?: d.hideTeacher,
        hideLocation = this.hide_location ?: d.hideLocation,
        hideDate = this.hide_date ?: d.hideDate,
        themeMode = this.theme_mode ?: d.themeMode,
        seedColor = this.seed_color
    )
}

fun WidgetStyle.toProto(): WidgetStyleProto {
    return WidgetStyleProto(
        font_scale = this.fontScale,
        background_alpha = this.backgroundAlpha,
        hide_teacher = this.hideTeacher,
        hide_location = this.hideLocation,
        hide_date = this.hideDate,
        theme_mode = this.themeMode,
        seed_color = this.seedColor
    )
}

/**
 * Protobuf -> ScheduleGridStyle 转换 function
 */
fun ScheduleGridStyleProto.toCompose(): ScheduleGridStyle {
    val d = ScheduleGridStyle.DEFAULT

    return ScheduleGridStyle(
        // 1. 基础布局尺寸
        timeColumnWidthDp = this.time_column_width_dp ?: d.timeColumnWidthDp,
        dayHeaderHeightDp = this.day_header_height_dp ?: d.dayHeaderHeightDp,
        sectionHeightDp = this.section_height_dp ?: d.sectionHeightDp,

        // 2. 课程块外观
        courseBlockCornerRadiusDp = this.course_block_corner_radius_dp ?: d.courseBlockCornerRadiusDp,
        courseBlockOuterPaddingDp = this.course_block_outer_padding_dp ?: d.courseBlockOuterPaddingDp,
        courseBlockInnerPaddingDp = this.course_block_inner_padding_dp ?: d.courseBlockInnerPaddingDp,

        // 3. 透明度与缩放
        courseBlockAlphaFloat = this.course_block_alpha_float ?: d.courseBlockAlphaFloat,
        courseBlockFontScale = this.course_block_font_scale ?: d.courseBlockFontScale,

        // 5. 列表转换
        courseColorMaps = if (this.course_color_maps.isEmpty()) d.courseColorMaps else this.course_color_maps.map { it.toCompose() },

        // 6. 开关映射
        hideGridLines = this.hide_grid_lines ?: d.hideGridLines,
        hideEmptyDays = this.hide_empty_days ?: d.hideEmptyDays,
        dimCompletedText = this.dim_completed_text ?: d.dimCompletedText,
        dimCompletedBackground = this.dim_completed_background ?: d.dimCompletedBackground,
        highlightOngoing = this.highlight_ongoing ?: d.highlightOngoing,
        strikeCompletedToday = this.strike_completed_today ?: d.strikeCompletedToday,
        hideReminderTip = this.hide_reminder_tip ?: d.hideReminderTip,
        hideSectionTime = this.hide_section_time ?: d.hideSectionTime,
        hideDateUnderDay = this.hide_date_under_day ?: d.hideDateUnderDay,
        showStartTime = this.show_start_time ?: d.showStartTime,
        hideLocation = this.hide_location ?: d.hideLocation,
        hideTeacher = this.hide_teacher ?: d.hideTeacher,
        removeLocationAt = this.remove_location_at ?: d.removeLocationAt,
        pageTextColorLong = this.page_text_color_long,
        courseTextColorLong = this.course_text_color_long,

        // 7. 对齐与边框
        textAlignCenterHorizontal = this.text_align_center_horizontal ?: d.textAlignCenterHorizontal,
        textAlignCenterVertical = this.text_align_center_vertical ?: d.textAlignCenterVertical,
        borderType = this.border_type ?: d.borderType,
        scheduleMode = this.schedule_mode ?: d.scheduleMode,

        // 8. 小组件独立样式映射
        widgetStyle = this.widget_style?.toCompose() ?: d.widgetStyle
    )
}

/**
 * ScheduleGridStyle -> Protobuf 转换 (用于写入)
 */
fun ScheduleGridStyle.toProto(): ScheduleGridStyleProto {
    return ScheduleGridStyleProto(
        time_column_width_dp = this.timeColumnWidthDp,
        day_header_height_dp = this.dayHeaderHeightDp,
        section_height_dp = this.sectionHeightDp,
        course_block_corner_radius_dp = this.courseBlockCornerRadiusDp,
        course_block_outer_padding_dp = this.courseBlockOuterPaddingDp,
        course_block_inner_padding_dp = this.courseBlockInnerPaddingDp,
        course_block_alpha_float = this.courseBlockAlphaFloat,
        course_block_font_scale = this.courseBlockFontScale,
        course_color_maps = this.courseColorMaps.map { it.toProto() },
        hide_grid_lines = this.hideGridLines,
        hide_empty_days = this.hideEmptyDays,
        dim_completed_text = this.dimCompletedText,
        dim_completed_background = this.dimCompletedBackground,
        highlight_ongoing = this.highlightOngoing,
        strike_completed_today = this.strikeCompletedToday,
        hide_reminder_tip = this.hideReminderTip,
        hide_section_time = this.hideSectionTime,
        hide_date_under_day = this.hideDateUnderDay,
        show_start_time = this.showStartTime,
        hide_location = this.hideLocation,
        hide_teacher = this.hideTeacher,
        remove_location_at = this.removeLocationAt,
        text_align_center_horizontal = this.textAlignCenterHorizontal,
        text_align_center_vertical = this.textAlignCenterVertical,
        border_type = this.borderType,
        schedule_mode = this.scheduleMode,

        page_text_color_long = this.pageTextColorLong,
        course_text_color_long = this.courseTextColorLong,
        widget_style = this.widgetStyle.toProto()
    )
}
