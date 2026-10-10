import java.util.Properties

plugins {
    alias(libs.plugins.jazzy.android.library)
    alias(libs.plugins.jazzy.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// The TMDB v4 read access token comes from local.properties (never committed) or the CI environment.
val tmdbToken: String = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}.getProperty("TMDB_TOKEN")?.trim()?.removeSurrounding("\"")
    ?: System.getenv("TMDB_TOKEN")
    ?: ""

android {
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        buildConfigField("String", "TMDB_TOKEN", "\"$tmdbToken\"")
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    api(libs.okhttp)
    implementation(libs.okhttp.logging)
    api(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.okhttp.mockwebserver)
}
