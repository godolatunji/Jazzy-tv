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
    implementation(projects.core.sources)
    implementation(projects.core.recommendations)
    api(projects.core.torrent)
    api(libs.androidx.paging.runtime)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    testImplementation(libs.okhttp.mockwebserver)
}
