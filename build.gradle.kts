// Root build file. App-level configuration lives in app/build.gradle.kts.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.ksp) apply false
}
