plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.hash.video"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        dataBinding = true
    }
}

dependencies {
    implementation(libs.androidx.media3.common) // Media3 通用媒体模型与播放器接口
    implementation(libs.androidx.media3.database) // 视频缓存索引的本地数据库实现
    implementation(libs.androidx.media3.datasource) // 网络数据源与边播边缓存数据源
    implementation(libs.androidx.media3.exoplayer) // ExoPlayer 播放、解码和缓冲内核
    implementation(libs.androidx.media3.session) // 后台播放服务、通知栏和系统媒体控制
    implementation(libs.androidx.media3.ui) // PlayerView 等视频播放界面组件
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1") // 播放状态的主线程收集
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
