package dev.satoshi.usefulyoutube.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("usefulyoutube-android-library"))
            pluginManager.apply(libs.pluginId("usefulyoutube-android-compose"))
            pluginManager.apply(libs.pluginId("usefulyoutube-hilt"))
            pluginManager.apply(libs.pluginId("kotlin-serialization"))

            dependencies {
                add("implementation", project(":core:model"))
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:ui"))
                add("implementation", libs.library("androidx-activity-compose"))
                add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-navigation-compose"))
                add("implementation", libs.library("kotlinx-serialization-json"))
                add("testImplementation", project(":core:testing"))
            }
        }
    }
}
