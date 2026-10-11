// E16-01 / E16-02: Baseline Profile generator and macrobenchmarks for :app.
//
//   ./gradlew :app:generateBaselineProfile                       -> app/src/release/generated/baselineProfiles/
//   ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest
//
// The baselineprofile plugin runs only @BaselineProfileGenerator-style tests (BaselineProfileRule)
// while generating, and only macrobenchmarks for the benchmark tasks.
// See docs/PERFORMANCE.md for device setup, emulator flags and how to read the results.
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.nexient.jazzy.baselineprofile"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    targetProjectPath = ":app"
}

baselineProfile {
    // Use whatever device is connected (pin one with ANDROID_SERIAL); no Gradle managed devices.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
