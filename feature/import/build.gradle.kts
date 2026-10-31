plugins {
    alias(libs.plugins.usefulyoutube.android.feature)
}

android {
    // "import" is a Kotlin keyword, so the package uses a descriptive name instead.
    namespace = "dev.satoshi.usefulyoutube.feature.subscriptionimport"
}
