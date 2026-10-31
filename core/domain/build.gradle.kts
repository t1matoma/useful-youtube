plugins {
    alias(libs.plugins.usefulyoutube.jvm.library)
    alias(libs.plugins.usefulyoutube.hilt)
    alias(libs.plugins.usefulyoutube.kover)
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)

    testImplementation(projects.core.testing)
}
