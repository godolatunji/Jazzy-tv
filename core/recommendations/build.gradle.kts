plugins {
    alias(libs.plugins.jazzy.android.library)
    alias(libs.plugins.jazzy.hilt)
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
}
