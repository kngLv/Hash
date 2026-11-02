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

    // 所有构建变体统一使用 Flutter Release 产物，避免提交大体积 Debug Engine。
    implementation("com.hash.hash_floorplan_module:flutter_release:1.0")
    implementation("com.getkeepsafe.relinker:relinker:1.4.5")
    implementation("com.google.android.play:core:1.10.3")
    implementation("androidx.webkit:webkit:1.15.0")
    implementation("androidx.datastore:datastore:1.1.7")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.preference:preference:1.2.1")

    testImplementation(libs.junit)

    kapt(libs.arouter.compiler)
}
