plugins {
    alias(libs.plugins.android.library)
    id("kotlin-kapt")
}

android {
    namespace = "com.hash.home"

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
    implementation(project(":core:net"))
    implementation(project(":core:bean"))
    implementation(project(":core:repository"))
    kapt(libs.arouter.compiler)
}