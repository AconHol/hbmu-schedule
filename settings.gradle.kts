// Modified by AconHol (2026): HBMU course-schedule learning project; upstream attribution retained.
pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/public")
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            name = "offlineCaptchaRuntime"
            url = uri("$rootDir/third_party/maven")
            content { includeModule("cn.hbmu.thirdparty", "onnxruntime-captcha") }
        }
        // The domestic mirror currently has metadata but no JVM binary for kgit 1.0.3.
        mavenCentral { content { includeGroup("io.github.xingheyuzhuan") } }
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "shiguangschedule"

include(":androidApp")
include(":shared")
include(":desktopApp")
