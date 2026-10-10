plugins {
    `kotlin-dsl`
}

group = "com.nexient.jazzy.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "jazzy.android.application"
            implementationClass = "com.nexient.jazzy.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "jazzy.android.library"
            implementationClass = "com.nexient.jazzy.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "jazzy.android.compose"
            implementationClass = "com.nexient.jazzy.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "jazzy.android.feature"
            implementationClass = "com.nexient.jazzy.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("hilt") {
            id = "jazzy.hilt"
            implementationClass = "com.nexient.jazzy.buildlogic.HiltConventionPlugin"
        }
        register("androidRoom") {
            id = "jazzy.android.room"
            implementationClass = "com.nexient.jazzy.buildlogic.AndroidRoomConventionPlugin"
        }
    }
}
