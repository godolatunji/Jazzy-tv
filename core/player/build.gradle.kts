plugins {
    alias(libs.plugins.jazzy.android.library)
    alias(libs.plugins.jazzy.android.compose)
    alias(libs.plugins.jazzy.hilt)
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    api(projects.core.torrent)
    implementation(projects.core.ffmpeg) // FfmpegAudioRenderer, picked up by DefaultRenderersFactory via reflection
    api(libs.androidx.media3.exoplayer)
    api(libs.androidx.media3.session)
    api(libs.androidx.media3.ui.compose)
    api(libs.androidx.media3.ui)
}
