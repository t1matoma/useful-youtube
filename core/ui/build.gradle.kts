plugins {
    alias(libs.plugins.usefulyoutube.android.library)
    alias(libs.plugins.usefulyoutube.android.compose)
}

android {
    namespace = "dev.satoshi.usefulyoutube.core.ui"
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
}
