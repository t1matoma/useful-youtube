package dev.satoshi.usefulyoutube.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Adds Hilt to Android modules and plain Dagger/Hilt annotations to JVM modules.
 */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("ksp"))
            dependencies {
                add("ksp", libs.library("hilt-compiler"))
            }

            pluginManager.withPlugin(libs.pluginId("kotlin-jvm")) {
                dependencies {
                    add("implementation", libs.library("hilt-core"))
                }
            }

            pluginManager.withPlugin("com.android.base") {
                pluginManager.apply(libs.pluginId("hilt"))
                dependencies {
                    add("implementation", libs.library("hilt-android"))
                }
            }
        }
    }
}
