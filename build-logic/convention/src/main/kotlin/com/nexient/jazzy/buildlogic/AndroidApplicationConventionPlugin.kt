package com.nexient.jazzy.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        extensions.configure<ApplicationExtension> {
            compileSdk = JazzySdk.COMPILE
            defaultConfig {
                minSdk = JazzySdk.MIN
                targetSdk = JazzySdk.TARGET
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                sourceCompatibility = JazzySdk.JAVA
                targetCompatibility = JazzySdk.JAVA
            }
            testOptions.unitTests.isIncludeAndroidResources = true
        }
    }
}
