plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.hash.widget"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {

}