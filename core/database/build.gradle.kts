plugins {
    alias(libs.plugins.usefulyoutube.android.library)
    alias(libs.plugins.usefulyoutube.android.room)
    alias(libs.plugins.usefulyoutube.hilt)
}

android {
    namespace = "dev.satoshi.usefulyoutube.core.database"
}

dependencies {
    api(projects.core.model)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}
