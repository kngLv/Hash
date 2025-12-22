import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.hash.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hash.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["AROUTER_MODULE_NAME"] = project.name
            }
        }
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
        debug {
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
        release {
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        dataBinding = true
    }
//
//    applicationVariants.all {
//        // apk 输出文件名配置
//        outputs.all {
//            val outputFileName = buildString {
//                append(rootProject.name)
//                append("_v")
//                append(versionName)
//                append("_")
//                append(buildType.name)
//                if (buildType.name == "release") {
//                    append("_")
//                    append(SimpleDateFormat("MMdd").format(Date()))
//                }
//                append(".apk")
//            }
//            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = outputFileName
//        }
//    }
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