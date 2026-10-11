plugins {
    alias(libs.plugins.jazzy.android.library)
    alias(libs.plugins.jazzy.hilt)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.network)
    implementation(projects.core.database)
    api(projects.core.sources)
    implementation(projects.core.recommendations)
    api(projects.core.torrent)
    implementation(projects.core.ffmpeg) // DeviceCapabilities.softwareAudio (DTS/TrueHD, PL-2)
    api(libs.androidx.paging.runtime)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    testImplementation(libs.okhttp.mockwebserver)
}
