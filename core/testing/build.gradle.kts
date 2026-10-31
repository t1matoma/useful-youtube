plugins {
    alias(libs.plugins.usefulyoutube.jvm.library)
}

dependencies {
    api(projects.core.model)
    api(projects.core.domain)
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
