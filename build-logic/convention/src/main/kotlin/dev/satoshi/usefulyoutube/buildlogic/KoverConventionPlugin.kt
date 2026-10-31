package dev.satoshi.usefulyoutube.buildlogic

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

private const val MINIMUM_LINE_COVERAGE_PERCENT = 80

/**
 * Enforces the minimum test coverage for the module as part of `check`.
 */
class KoverConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("kover"))

            extensions.configure<KoverProjectExtension> {
                reports {
                    filters {
                        excludes {
                            annotatedBy("dagger.Module", "javax.annotation.processing.Generated")
                            classes(
                                "*_Factory",
                                "*_Factory\$*",
                                "*_MembersInjector",
                                "*_Impl",
                                "*_Impl\$*",
                                "*.BuildConfig",
                            )
                        }
                    }
                    verify {
                        rule {
                            minBound(MINIMUM_LINE_COVERAGE_PERCENT)
                        }
                    }
                }
            }

            tasks.named("check") {
                dependsOn("koverVerify")
            }
        }
    }
}
