// Media3 FFmpeg audio decoder (PL-2): DTS / Dolby TrueHD software decoding.
// Java sources are Media3's libraries/decoder_ffmpeg at 1.11.1 (Apache-2.0, unchanged).
// Native libs in src/main/jniLibs are prebuilt from pinned sources by build-ffmpeg/build.sh
// (FFmpeg LGPL-2.1-or-later, shared/dynamically linked) so builds and CI don't need the NDK.
// See LICENSE-NOTICE.md.
plugins {
    alias(libs.plugins.jazzy.android.library)
}

android {
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
    lint {
        // The sources are Media3's own, which use @UnstableApi APIs without opting in (Media3 marks
        // the classes themselves @UnstableApi). Kept unchanged, so the check is off for this module only.
        disable += "UnsafeOptInUsageError"
    }
}

dependencies {
    api(libs.androidx.media3.decoder)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.annotation)
    compileOnly(libs.checkerframework.qual)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.truth)
}
