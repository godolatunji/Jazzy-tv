plugins {
    alias(libs.plugins.jazzy.android.library)
    alias(libs.plugins.jazzy.hilt)
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    api(libs.libtorrent4j)
    implementation(libs.libtorrent4j.android.arm64)
    implementation(libs.libtorrent4j.android.arm)
    implementation(libs.libtorrent4j.android.amd64)
    implementation(libs.libtorrent4j.android.x86)
    api(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.nanohttpd)
}
