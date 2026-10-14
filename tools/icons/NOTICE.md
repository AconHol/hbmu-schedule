# 图标来源

应用内的操作图标使用 Google Material Symbols Rounded，24 dp 光学尺寸、400 字重；导航使用同一套图标的空心和实心状态。矢量路径从官方 SVG 转换，保持原形状，调整坐标原点为 Android VectorDrawable 使用的原点。每个图标的来源与转换前后 SHA-256 保存在 `material-symbols.json`。

XML 文本哈希按 UTF-8、LF 换行计算，以兼容 Git 在 Windows 工作区转换 CRLF 的行为；SVG 哈希为下载内容的原始字节哈希。

Copyright Google LLC. Licensed under the Apache License, Version 2.0. 完整许可证随应用打包于 `shared/src/commonMain/composeResources/files/licenses/Material-Symbols-Apache-2.0.txt`。

桌面图标和应用内的课程日历品牌标记为本项目绘制，与上述 Google 操作图标区分。品牌标记前景使用当前主题颜色；系统桌面通过自适应图标的单色层着色。

官方说明：https://developers.google.com/fonts/docs/material_symbols

运行 `python tools/icons/sync_material_symbols.py` 可重新导入所需图标。脚本优先使用 Google 国内 CDN，下载全部所需图标完成后才写入资源。APK 仅打包静态矢量，不依赖图标字体或运行时下载。
