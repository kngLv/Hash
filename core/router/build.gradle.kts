plugins {
    alias(libs.plugins.android.library)
    id("kotlin-kapt")
}

android {
    namespace = "com.hash.router"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["AROUTER_MODULE_NAME"] = project.name
            }
        }
    }
}

dependencies {
    implementation(project(":core:common"))

    api(libs.arouter.api)
    kapt(libs.arouter.compiler)
}