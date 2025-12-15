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
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
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
