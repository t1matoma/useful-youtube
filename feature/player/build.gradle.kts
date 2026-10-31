plugins {
    alias(libs.plugins.usefulyoutube.android.feature)
}

android {
    namespace = "dev.satoshi.usefulyoutube.feature.player"
}

dependencies {
    implementation(libs.youtube.player)
}
