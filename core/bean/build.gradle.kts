plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.hash.bean"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {

}