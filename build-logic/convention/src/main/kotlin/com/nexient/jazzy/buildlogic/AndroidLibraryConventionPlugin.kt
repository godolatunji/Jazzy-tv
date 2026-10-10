package com.nexient.jazzy.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        extensions.configure<LibraryExtension> {
            namespace = jazzyNamespace()
            compileSdk = JazzySdk.COMPILE
            defaultConfig {
                minSdk = JazzySdk.MIN
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                sourceCompatibility = JazzySdk.JAVA
                targetCompatibility = JazzySdk.JAVA
            }
            testOptions.unitTests.isIncludeAndroidResources = true
        }
        dependencies {
            add("implementation", libs.lib("kotlinx-coroutines-android"))
            add("testImplementation", libs.lib("junit"))
            add("testImplementation", libs.lib("kotlinx-coroutines-test"))
            add("testImplementation", libs.lib("turbine"))
            add("testImplementation", libs.lib("truth"))
        }
    }
}
