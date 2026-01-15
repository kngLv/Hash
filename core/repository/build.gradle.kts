plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.hash.repository"

    defaultConfig {
    }

}

dependencies {
    implementation(project(":core:net"))
    implementation(project(":core:common"))
    implementation(project(":core:bean"))
}

