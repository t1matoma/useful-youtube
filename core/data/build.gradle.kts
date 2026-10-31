plugins {
    alias(libs.plugins.usefulyoutube.android.library)
    alias(libs.plugins.usefulyoutube.hilt)
    alias(libs.plugins.usefulyoutube.kover)
}

android {
    namespace = "dev.satoshi.usefulyoutube.core.data"
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.database)
    implementation(projects.core.network)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.runtime)
}
