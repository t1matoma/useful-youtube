plugins {
    alias(libs.plugins.usefulyoutube.android.library)
    alias(libs.plugins.usefulyoutube.hilt)
}

android {
    namespace = "dev.satoshi.usefulyoutube.core.auth"
}

dependencies {
    api(projects.core.domain)

    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.coroutines.play.services)
}
