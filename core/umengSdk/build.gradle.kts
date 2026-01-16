plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.hash.umengsdk"

    buildFeatures {
        // 是否生成 BuildConfig 类
        buildConfig = true
    }

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "UM_KEY", "\"6969921a9a7f376488336cf2\"")
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("com.umeng.umsdk:common:9.8.9")
    implementation("com.umeng.umsdk:asms:1.8.7.2")
}