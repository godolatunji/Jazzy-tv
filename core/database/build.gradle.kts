plugins {
    alias(libs.plugins.jazzy.android.library)
    alias(libs.plugins.jazzy.android.room)
    alias(libs.plugins.jazzy.hilt)
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    api(libs.androidx.room.paging)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
