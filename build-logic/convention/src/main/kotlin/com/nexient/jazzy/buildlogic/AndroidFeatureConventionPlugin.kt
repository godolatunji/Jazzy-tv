package com.nexient.jazzy.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * A feature module: Compose UI + Hilt ViewModels + the shared core modules.
 * Feature modules never depend on each other; the app wires navigation between them.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("jazzy.android.library")
        pluginManager.apply("jazzy.android.compose")
        pluginManager.apply("jazzy.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        dependencies {
            add("implementation", project(":core:model"))
            add("implementation", project(":core:common"))
            add("implementation", project(":core:data"))
            add("implementation", project(":core:designsystem"))
            add("implementation", libs.lib("androidx-activity-compose"))
            add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("androidx-hilt-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("androidx-navigation3-runtime"))
            add("implementation", libs.lib("kotlinx-serialization-json"))
            add("implementation", libs.lib("androidx-compose-material-icons-extended"))
            add("implementation", libs.lib("androidx-tv-material"))
            add("implementation", libs.lib("coil-compose"))
        }
    }
}
