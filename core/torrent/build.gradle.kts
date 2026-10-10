plugins {
    alias(libs.plugins.jazzy.android.library)
    alias(libs.plugins.jazzy.hilt)
}

android {
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    // StreamRequest/StreamStats/EngineSettings appear in the public TorrentEngine API.
    api(projects.core.model)
    implementation(projects.core.common)
    api(libs.libtorrent4j)
    implementation(libs.libtorrent4j.android.arm64)
    implementation(libs.libtorrent4j.android.arm)
    implementation(libs.libtorrent4j.android.amd64)
    implementation(libs.libtorrent4j.android.x86)
    api(libs.androidx.media3.common)
    // TorrentDataSource / TorrentAwareDataSourceFactory expose media3-datasource types.
    api(libs.androidx.media3.exoplayer)
    implementation(libs.nanohttpd)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
