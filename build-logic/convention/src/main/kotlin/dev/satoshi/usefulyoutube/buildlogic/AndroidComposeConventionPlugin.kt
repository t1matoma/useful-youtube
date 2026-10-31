package dev.satoshi.usefulyoutube.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("kotlin-compose"))

            extensions.getByType<CommonExtension>().buildFeatures.compose = true

            dependencies {
                val composeBom = platform(libs.library("androidx-compose-bom"))
                add("implementation", composeBom)
                add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
                add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
                add("testImplementation", composeBom)
                add("testImplementation", libs.library("androidx-compose-ui-test-junit4"))
                add("testImplementation", libs.library("robolectric"))
                add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
            }
        }
    }
}
