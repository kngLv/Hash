plugins {
    alias(libs.plugins.android.library)
    id("kotlin-kapt")
}

android {
    namespace = "com.hash.discover"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["AROUTER_MODULE_NAME"] = project.name
            }
        }
    }

    buildFeatures {
        dataBinding = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:router"))
    kapt(libs.arouter.compiler)
}