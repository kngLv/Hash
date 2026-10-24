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
    implementation(project(":core:router"))
    implementation("com.google.code.gson:gson:2.13.2")
}

