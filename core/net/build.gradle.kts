plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.hash.net"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:bean"))
    implementation("com.google.code.gson:gson:2.13.2")

    api("com.squareup.okhttp3:okhttp:4.12.0")
    api("com.squareup.retrofit2:retrofit:2.9.0")
//    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    //协程基础库
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    //协程 Android 库，提供 UI 调度器
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
//
//    api("com.github.LvKang-insist:LvHttp:1.2.0")

}