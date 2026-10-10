plugins {
    alias(libs.plugins.jazzy.android.library)
    alias(libs.plugins.jazzy.hilt)
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.okhttp.mockwebserver)
}

tasks.withType<Test>().configureEach {
    // Opt-in live site check: -PliveSmoke=true (see LiveProvidersSmokeTest). Never runs in CI by default.
    systemProperty("liveSmoke", providers.gradleProperty("liveSmoke").getOrElse("false"))
    systemProperty("liveQuery", providers.gradleProperty("liveQuery").getOrElse(""))
    testLogging.showStandardStreams = providers.gradleProperty("liveSmoke").isPresent
}
