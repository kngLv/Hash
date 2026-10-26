pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo1.maven.org/maven2/")}
    }

    // 如果 VasDolly 插件没有发布 Gradle 插件标记（plugin marker），
    // 则把插件 id 映射到实际的模块坐标，这样 plugins DSL 仍然可以通过该模块坐标解析并下载插件。
    // 举例：插件 id "com.tencent.vasdolly" 映射到模块坐标 "com.tencent.vasdolly:plugin:3.0.6"。
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.tencent.vasdolly") {
                useModule("com.tencent.vasdolly:plugin:3.0.6")
            }
        }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo1.maven.org/maven2/")}
    }
}

rootProject.name = "HashApp"
include(":app")
include("core:common")
include(":core:widget")
include(":core:router")
include("feature:main")
include(":feature:home")
include(":feature:discover")
include(":feature:release")
include(":feature:msg")
include(":feature:mine")
include(":feature:login")

include(":core:net")
include(":core:bean")
include(":core:agora")
include(":core:database")
include(":core:repository")
include(":core:umengSdk")
