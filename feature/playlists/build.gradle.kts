plugins {
    alias(libs.plugins.usefulyoutube.android.feature)
}

android {
    namespace = "dev.satoshi.usefulyoutube.feature.playlists"
}

dependencies {
    implementation(projects.core.auth)
}
