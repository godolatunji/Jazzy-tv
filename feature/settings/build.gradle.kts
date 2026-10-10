plugins {
    alias(libs.plugins.jazzy.android.feature)
}

dependencies {
    // UnifiedGenre.pickable for "Favourite genres".
    implementation(projects.core.recommendations)
}
