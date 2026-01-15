plugins {
    alias(libs.plugins.android.library)
    id("kotlin-kapt")
}

android {
    namespace = "com.hash.database"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

}

kapt {
    // Export Room database schema to a module-local directory to satisfy Room's requirement
    arguments {
        // 导出到项目根目录下的 schemas/core-database
        arg("room.schemaLocation", "${rootProject.projectDir}/schemas/core-database")
    }
}

dependencies {
    api("androidx.room:room-runtime:2.8.4")
    api("androidx.room:room-ktx:2.8.4")
    kapt("androidx.room:room-compiler:2.8.4")
}