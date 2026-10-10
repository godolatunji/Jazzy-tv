plugins {
    alias(libs.plugins.jazzy.android.library)
    alias(libs.plugins.jazzy.android.compose)
}

dependencies {
    implementation(projects.core.model)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.androidx.tv.material)
    api(libs.coil.compose)
    api(libs.androidx.paging.compose)
}
