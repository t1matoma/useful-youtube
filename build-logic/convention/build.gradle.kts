import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

group = "dev.satoshi.usefulyoutube.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.hilt.gradle.plugin)
    compileOnly(libs.room.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
    implementation(libs.kover.gradle.plugin)
}

ktlint {
    version = libs.versions.ktlint.get()
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootDir.resolve("../config/detekt/detekt.yml"))
}

configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(libs.versions.detektKotlin.get())
        }
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = JvmTarget.JVM_17.target
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id =
                libs.plugins.usefulyoutube.android.application
                    .get()
                    .pluginId
            implementationClass = "dev.satoshi.usefulyoutube.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id =
                libs.plugins.usefulyoutube.android.library
                    .get()
                    .pluginId
            implementationClass = "dev.satoshi.usefulyoutube.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id =
                libs.plugins.usefulyoutube.android.compose
                    .get()
                    .pluginId
            implementationClass = "dev.satoshi.usefulyoutube.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id =
                libs.plugins.usefulyoutube.android.feature
                    .get()
                    .pluginId
            implementationClass = "dev.satoshi.usefulyoutube.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("androidRoom") {
            id =
                libs.plugins.usefulyoutube.android.room
                    .get()
                    .pluginId
            implementationClass = "dev.satoshi.usefulyoutube.buildlogic.AndroidRoomConventionPlugin"
        }
        register("hilt") {
            id =
                libs.plugins.usefulyoutube.hilt
                    .get()
                    .pluginId
            implementationClass = "dev.satoshi.usefulyoutube.buildlogic.HiltConventionPlugin"
        }
        register("jvmLibrary") {
            id =
                libs.plugins.usefulyoutube.jvm.library
                    .get()
                    .pluginId
            implementationClass = "dev.satoshi.usefulyoutube.buildlogic.JvmLibraryConventionPlugin"
        }
        register("kover") {
            id =
                libs.plugins.usefulyoutube.kover
                    .get()
                    .pluginId
            implementationClass = "dev.satoshi.usefulyoutube.buildlogic.KoverConventionPlugin"
        }
        register("quality") {
            id =
                libs.plugins.usefulyoutube.quality
                    .get()
                    .pluginId
            implementationClass = "dev.satoshi.usefulyoutube.buildlogic.QualityConventionPlugin"
        }
    }
}
