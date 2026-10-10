plugins {
    alias(libs.plugins.jazzy.android.feature)
}

dependencies {
    // UnifiedGenre.pickable for the "Pick 3 or more genres you like" step (PR-3).
    implementation(projects.core.recommendations)
}
