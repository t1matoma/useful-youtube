import java.io.StringReader
import java.util.Properties

plugins {
    alias(libs.plugins.usefulyoutube.android.library)
    alias(libs.plugins.usefulyoutube.hilt)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties =
    Properties().apply {
        providers
            .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
            .asText
            .orNull
            ?.let { load(StringReader(it)) }
    }

android {
    namespace = "dev.satoshi.usefulyoutube.core.network"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        val apiKey = localProperties.getProperty("youtube.api.key").orEmpty()
        buildConfigField("String", "YOUTUBE_API_KEY", "\"$apiKey\"")
    }
}

dependencies {
    api(projects.core.model)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.coroutines)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.xmlutil.serialization)

    testImplementation(libs.okhttp.mockwebserver)
}
