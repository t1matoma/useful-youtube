plugins {
    alias(libs.plugins.usefulyoutube.android.application)
    alias(libs.plugins.usefulyoutube.android.compose)
    alias(libs.plugins.usefulyoutube.hilt)
}

android {
    namespace = "dev.satoshi.usefulyoutube"

    defaultConfig {
        applicationId = "dev.satoshi.usefulyoutube"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(projects.core.auth)
    implementation(projects.core.data)
    implementation(projects.core.ui)
    implementation(projects.feature.feed)
    implementation(projects.feature.player)
    implementation(projects.feature.channels)
    implementation(projects.feature.import)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)
}
