plugins {
    alias(libs.plugins.usefulyoutube.android.library)
    alias(libs.plugins.usefulyoutube.android.room)
    alias(libs.plugins.usefulyoutube.hilt)
}

android {
    namespace = "dev.satoshi.usefulyoutube.core.database"
}

dependencies {
    implementation(projects.core.model)
}
