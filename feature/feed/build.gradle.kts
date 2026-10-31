plugins {
    alias(libs.plugins.usefulyoutube.android.feature)
}

android {
    namespace = "dev.satoshi.usefulyoutube.feature.feed"
}

dependencies {
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
