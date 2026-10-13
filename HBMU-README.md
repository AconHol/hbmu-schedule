<!-- Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained. -->
# 课程表使用说明

**个人开发学习与研究项目，非学校官方产品。开发者不会私自盗取、收集或分享用户信息。** 请先阅读 [学习用途与使用边界](DISCLAIMER.md) 和 [隐私承诺](PRIVACY.md)，仅处理本人有权使用的数据。

## 开始使用

1. 安装相应架构的 APK。现代手机优先使用 `arm64-v8a`，不确定时使用 `universal`。
2. 在“我的”中完成本人账号的设置，按页面提示操作。
3. 选择所需学期，完成课表设置后可离线查看今日与每周安排。
4. 在“作息管理”中校准实际上下课时间。
5. 按需允许通知权限并设置上课提醒与明日课表预告。

应用名称为“课程表”，包名为 `cn.hbmu.schedule`，支持 Android 8.0 及以上，可与原版拾光课程表同时安装。

## 日常使用

今日页突出当天与下一节课程，周课表支持上一周、下一周、周次选择及“回到本周”。账号卡片在离线时仍可显示本机保存的信息。连接问题在“我的”中处理，主页保持清晰。

保持登录与自动处理由用户自己决定。退出账号会清除登录状态，本地课表仍可离线查看。若需保留手动修改的安排，可另建课表以免被后续课表更新覆盖。

课程编辑、多课表、作息管理、节假日、导入导出、日历、小组件、备份和原有工具保留在“工具与设置”中。用户主动配置的备份和导出由自己控制，开发者不会收取这些数据。

## 提醒

上课提醒默认提前 15 分钟。明日预告默认前一晚 20:00，可选 18:00、20:00、21:00；明天没有课程时不发送预告。首次完成课表设置后主动申请通知权限，拒绝后仍可查看课表。

提醒以课表作息为基础，可能受通知权限、电量管理和系统调度影响。请以学校官方信息为准。

## 构建与验证

需要 JDK 21、Android SDK 37 和 Gradle 9.7.1。配置 SDK 路径后运行：

```powershell
.\gradlew.bat :shared:testAndroidHostTest :androidApp:assembleRelease :androidApp:lintRelease --no-configuration-cache
```

release 签名通过自己的 `HBMU_KEYSTORE_PATH` 与 `HBMU_STORE_PASSWORD` 环境变量设置，密钥别名为 `hbmu`。未设置时使用开发调试签名，不能覆盖维护者签名的安装包。维护者的私钥与密码不公开。

版本为 `2.1.0-hbmu.4`，versionCode 为 `40`。Android 为当前维护平台，桌面与 iOS 的相关功能尚未验证。构建、18 项 Kotlin 单元测试、release lint 与签名检查通过；页面预览来自共享 Compose 代码渲染并使用虚构数据。Android 实机安装、权限弹窗和通知实际送达尚未完成设备验证，当前为预发布版。

上游来源与许可证见 [README.md](README.md) 和 [NOTICE](NOTICE)，版本改动见 [APP-UPDATE.md](APP-UPDATE.md)。
