package dev.satoshi.usefulyoutube.buildlogic

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jlleitschuh.gradle.ktlint.KtlintExtension

private const val DETEKT_JVM_TARGET = "17"

/**
 * Applies ktlint and detekt with the shared configuration from the repository root.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("ktlint"))
            pluginManager.apply(libs.pluginId("detekt"))

            extensions.configure<KtlintExtension> {
                version.set(libs.version("ktlint"))
            }

            extensions.configure<DetektExtension> {
                buildUponDefaultConfig = true
                parallel = false
                config.setFrom(rootProject.file("config/detekt/detekt.yml"))
                source.setFrom(
                    "src/main/kotlin",
                    "src/test/kotlin",
                    "src/androidTest/kotlin",
                )
            }

            tasks.withType<Detekt>().configureEach {
                jvmTarget = DETEKT_JVM_TARGET
            }

            val detektKotlinVersion = libs.version("detektKotlin")
            configurations.matching { it.name == "detekt" }.configureEach {
                resolutionStrategy.eachDependency {
                    if (requested.group == "org.jetbrains.kotlin") {
                        useVersion(detektKotlinVersion)
                    }
                }
            }
        }
    }
}
