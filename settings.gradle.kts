pluginManagement {
    includeBuild("build-logic")
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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Jazzy"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

include(":core:model")
include(":core:common")
include(":core:network")
include(":core:sources")
include(":core:database")
include(":core:data")
include(":core:recommendations")
include(":core:torrent")
include(":core:player")
include(":core:designsystem")

include(":feature:profiles")
include(":feature:home")
include(":feature:details")
include(":feature:search")
include(":feature:discover")
include(":feature:library")
include(":feature:player")
include(":feature:settings")
