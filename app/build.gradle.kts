plugins {
    alias(libs.plugins.jazzy.android.application)
    alias(libs.plugins.jazzy.android.compose)
    alias(libs.plugins.jazzy.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.nexient.jazzy"

    defaultConfig {
        applicationId = "com.nexient.jazzy"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        create("internal") {
            initWith(getByName("debug"))
            isDebuggable = false
            matchingFallbacks += listOf("debug")
            optimization {
                enable = true
            }
        }
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.player)

    implementation(projects.feature.profiles)
    implementation(projects.feature.home)
    implementation(projects.feature.details)
    implementation(projects.feature.search)
    implementation(projects.feature.discover)
    implementation(projects.feature.library)
    implementation(projects.feature.player)
    implementation(projects.feature.settings)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.tv.material)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
