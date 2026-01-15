plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.hash.agora"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

}

dependencies {

    implementation(project(":core:common"))

    api("io.agora:agora-rtm:2.2.2")
}