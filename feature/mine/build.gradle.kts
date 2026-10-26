plugins {
    alias(libs.plugins.android.library)
    id("kotlin-kapt")
}

android {
    namespace = "com.hash.mine"

    defaultConfig {

        consumerProguardFiles("consumer-rules.pro")

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["AROUTER_MODULE_NAME"] = project.name
            }
        }
    }
    buildFeatures {
        dataBinding = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:net"))
    implementation(project(":core:router"))

    // 户型 Flutter 模块及其插件、arm64 Flutter Engine 均直接随 Mine 模块保存。
    implementation(
        files(
            "libs/floorplan/flutter_debug-1.0.aar",
            "libs/floorplan/hash_floorplan_debug-1.0.aar",
            "libs/floorplan/shared_preferences_android_debug-1.0.aar",
            "libs/floorplan/webview_flutter_android_debug-1.0.aar",
            "libs/floorplan/flutter_embedding_debug.jar",
            "libs/floorplan/flutter_arm64_v8a_debug.jar",
        ),
    )
    implementation("androidx.webkit:webkit:1.15.0")
    implementation("androidx.datastore:datastore:1.1.7")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.preference:preference:1.2.1")

    testImplementation(libs.junit)

    kapt(libs.arouter.compiler)
}
