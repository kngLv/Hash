import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    id("com.tencent.vasdolly")
    id("kotlin-kapt")
}

android {
    namespace = "com.hash.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hash.app"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["AROUTER_MODULE_NAME"] = project.name
            }
        }
    }

    buildFeatures {
        dataBinding = true
    }

    signingConfigs {
        signingConfigs {
            create("config") {
                storeFile = file(project.findProperty("StoreFile") as String)
                storePassword = project.findProperty("StorePassword") as String
                keyAlias = project.findProperty("KeyAlias") as String
                keyPassword = project.findProperty("KeyPassword") as String
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            isJniDebuggable = true
            isShrinkResources = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("config")
            manifestPlaceholders["app_name"] = "Hash Debug 版"
            // 调试模式下只保留一种架构的 so 库，提升打包速度
            ndk {
                abiFilters.add("arm64-v8a")
            }
        }
        getByName("preview") {
            initWith(getByName("debug"))
            applicationIdSuffix = ""
            // 添加清单占位符
            manifestPlaceholders += mapOf(
                "app_name" to "@string/app_name_preview"
            )
        }
        getByName("release") {
            isDebuggable = false// 调试模式开关
            isJniDebuggable = false
            isShrinkResources = true// 移除无用的资源
            isMinifyEnabled = true// 代码混淆开关
            signingConfig = signingConfigs.getByName("config")// 签名信息配置
            manifestPlaceholders["app_name"] = "@string/app_name"// 添加清单占位符
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // 仅保留两种架构的 so 库，根据 BugLy 统计得出
            ndk {
                // armeabi：万金油架构平台（占用率：0%）
                // armeabi-v7a：曾经主流的架构平台（占用率：10%）`
                // arm64-v8a：目前主流架构平台（占用率：95%）
                abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
            }
        }
    }
}

// 执行 ./gradlew :app:rebuildChannel 生成渠道包
rebuildChannel {
    //指定渠道文件
    channelFile = File(project.rootDir, "channel.txt")
    // 已有APK文件地址（必填）,如new File(project.rootDir, "/baseApk/app_base.apk"),文件名中的base将被替换为渠道名
//    baseApk = File(project.rootDir, "app/release/app_base.apk")
    baseApk = File("/Users/lvkang/StudioProjects/Hash/app/release/app_base.apk")
    //默认为new File(project.buildDir, "rebuildChannel")
//    outputDir = 渠道包输出目录
    //快速模式：生成渠道包时不进行校验（速度可以提升10倍以上，默认为false）
    fastMode = false
    //低内存模式（仅针对V2签名，默认为false）：只把签名块、中央目录和EOCD读取到内存，不把最大头的内容块读取到内存，在手机上合成APK时，可以使用该模式
    lowMemory = false
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:router"))
    implementation(project(":core:net"))

    implementation(project(":feature:main"))
    implementation(project(":feature:release"))
    implementation(project(":feature:home"))
    implementation(project(":feature:discover"))
    implementation(project(":feature:msg"))
    implementation(project(":feature:mine"))
    implementation(project(":feature:login"))
    kapt(libs.arouter.compiler)
}