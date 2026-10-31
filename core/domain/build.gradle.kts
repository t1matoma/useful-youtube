plugins {
    alias(libs.plugins.usefulyoutube.jvm.library)
    alias(libs.plugins.usefulyoutube.kover)
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)

    testImplementation(projects.core.testing)
}
