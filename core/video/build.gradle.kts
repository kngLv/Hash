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
    implementation(libs.androidx.activity.ktx) // Activity 级视频覆盖层与返回键协调
    implementation(libs.androidx.core.ktx) // 全屏页面的系统栏兼容控制
    implementation(libs.androidx.lifecycle.process) // 根据应用前后台切换队列续播与通知操作
    implementation(libs.androidx.media3.common) // Media3 通用媒体模型与播放器接口
    implementation(libs.androidx.media3.database) // 视频缓存索引的本地数据库实现
    implementation(libs.androidx.media3.datasource) // 网络数据源与边播边缓存数据源
    implementation(libs.androidx.media3.exoplayer) // ExoPlayer 播放、解码和缓冲内核
    implementation(libs.androidx.media3.exoplayer.hls) // HLS（m3u8）媒体源支持
    implementation(libs.androidx.media3.session) // 后台播放服务、通知栏和系统媒体控制
    implementation(libs.androidx.media3.ui) // PlayerView 等视频播放界面组件
    implementation(libs.glide) // 内嵌播放器的封面加载与尺寸读取
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1") // 播放状态的主线程收集
    testImplementation(libs.junit)
    androidTestImplementation("androidx.test:core:1.6.1") // ActivityScenario 驱动呈现宿主设备测试
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0") // 验证视频请求头是否真正发出
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
