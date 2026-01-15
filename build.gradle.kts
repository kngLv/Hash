import com.android.build.gradle.BaseExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
}

subprojects {
    pluginManager.withPlugin("com.android.base") {
        // 为所有模块应用 Kotlin 插件
        apply(plugin = "org.jetbrains.kotlin.android")

        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_21)
                freeCompilerArgs.add("-Xcontext-parameters")
            }
        }
        extensions.configure<BaseExtension> {
            compileSdkVersion(36)
            defaultConfig {
                minSdk = 24
                // Android 版本适配指南：https://github.com/getActivity/AndroidVersionAdapter
                targetSdk = 35
                versionCode = 1
                versionName = "1.0.0"
            }
            compileOptions {
                targetCompatibility = JavaVersion.VERSION_21
                sourceCompatibility = JavaVersion.VERSION_21
            }
            buildTypes {
                getByName("debug") {}
                create("preview") {
                    matchingFallbacks += listOf("debug")
                }
                getByName("release") {}
            }
        }

        // 通用依赖配置（排除 library:base，因为它使用 api 依赖）
        dependencies {
            // 依赖 libs 目录下所有的 jar 和 aar 包
            // implementation(fileTree(mapOf("include" to listOf("*.jar", "*.aar"), "dir" to "libs")))
            // add("implementation", fileTree(mapOf("include" to listOf("*.jar", "*.aar"), "dir" to "libs")))
            add("implementation", libs.androidx.appcompat)
            add("implementation", libs.androidx.core.ktx)
            add("implementation", libs.material)

        }
    }
}