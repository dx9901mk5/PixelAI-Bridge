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
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PixelAI-Bridge"
include(":app")
include(":core:designsystem")
include(":core:ai")
include(":core:shizuku")
include(":feature:screenshots")
include(":feature:nowplaying")
include(":feature:magiccue")
include(":feature:studio")
include(":feature:callnotes")
