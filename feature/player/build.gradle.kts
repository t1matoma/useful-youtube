plugins {
    alias(libs.plugins.usefulyoutube.android.feature)
}

android {
    namespace = "dev.satoshi.usefulyoutube.feature.player"
}

dependencies {
    implementation(projects.core.auth)
    implementation(libs.youtube.player)
}
