plugins {
    alias(libs.plugins.usefulyoutube.android.library)
    alias(libs.plugins.usefulyoutube.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.satoshi.usefulyoutube.core.network"
}

dependencies {
    implementation(projects.core.model)
}
