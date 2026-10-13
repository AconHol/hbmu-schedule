<!-- Modified by AconHol (2026): implementation references for the expressive UI. -->
# 2.2.0 界面实现与参考

![页面与胶囊切换演示](docs/navigation-motion.gif)

演示由共享 Compose 代码渲染，使用虚构课表。桌面预览展示磨砂兼容效果；Android 13 及以上使用液态玻璃渲染。

应用使用官方 Compose Material 3 的 `MaterialExpressiveTheme`、`MotionScheme.expressive()`、`NavigationBar`、`NavigationBarItem`、`Surface`、`Card`、按钮、图标按钮与列表组件。配色读取当前主题的语义色角色，保留动态取色与浅色、深色主题。

## 设计与行为

- 底栏使用官方导航项与可持续移动的单一胶囊选中层；点击和拖动均保持三个主目的地的顺序。正文切换按“今日 → 课表 → 我的”的空间关系决定方向，详情页面的前进与返回反向过渡。
- Android 13 及以上的底栏使用 Backdrop 的真实背景采样、模糊、色彩调整与边缘折射。旧 Android 与桌面预览使用 Haze 磨砂兼容效果。玻璃材质属于应用的定制视觉处理，并非宣称 Material 官方提供了液态玻璃导航栏。
- 下一节课使用 `primaryContainer / onPrimaryContainer`，重要操作使用 `primary / onPrimary`，取消大面积高饱和主色块。自定义主题采用 Tonal Spot 配色，以保留用户选择的主色色相；Expressive 组件、形状与动效仍使用官方接口。
- 课表重写为安静的时间轴、独立日期表头、弱分隔线与紧凑圆角课程卡片。MD3 Expressive 没有现成的课程表网格组件；课程布局使用自定义 Compose `Layout`，内容使用 Material 的主题、文字与表面角色。
- 五天和七天视图会按可用宽度与系统字号调整网格宽度，窄屏允许水平滚动。保留课程重叠布局、长按拖动、时间调整和课程详情；课程支持辅助功能点击语义。
- 学习用途与隐私说明移入“工具与设置 → 用途与隐私”。贡献者使用离线打包数据、首字母头像与分组列表，不依赖网络下载头像。

## 官方参考

- [Material 3 与 Expressive 的 Compose 实现](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Material 导航栏](https://m3.material.io/components/navigation-bar/overview)
- [Compose 导航栏组件](https://developer.android.com/develop/ui/compose/components/navigation-bar)
- [Material Expressive 更新](https://m3.material.io/blog/building-with-m3-expressive)
- [Material 布局](https://m3.material.io/foundations/layout/layout-overview/overview)
- [网格与信息密度](https://m3.material.io/foundations/layout/grids-spacing/density)
- [间距系统](https://m3.material.io/foundations/layout/grids-spacing/spacing)
- [Material 动效](https://m3.material.io/styles/motion/overview/how-it-works)
- [Compose 辅助功能默认行为](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)

## 第三方材质参考与许可

液态玻璃参考 [Kyant0/AndroidLiquidGlass 的 LiquidBottomTabs 示例](https://github.com/Kyant0/AndroidLiquidGlass/blob/1.0.6/catalog/src/main/java/com/kyant/backdrop/catalog/components/LiquidBottomTabs.kt)，实际引入 `io.github.kyant0:backdrop:1.0.6`，作者 Kyant0，许可 Apache 2.0。旧系统磨砂效果使用 Chris Banes 的 Haze（Apache 2.0）。原有上游署名、源码许可与其他第三方许可保留。

用户提供的 MD3E 音乐界面截图仅作为交互与视觉参考，不将其封面、照片或其他素材放入应用。
