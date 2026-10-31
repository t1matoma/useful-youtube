package dev.satoshi.usefulyoutube.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

private val javaVersion = JavaVersion.VERSION_17
private val kotlinJvmTarget = JvmTarget.JVM_17
private const val JDK_TOOLCHAIN_VERSION = 21
private const val TEST_MAX_HEAP_SIZE = "1g"

internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = libs.version("compileSdk").toInt()
        defaultConfig.minSdk = libs.version("minSdk").toInt()
        compileOptions.sourceCompatibility = javaVersion
        compileOptions.targetCompatibility = javaVersion
        testOptions.unitTests.isIncludeAndroidResources = true
    }
    extensions.configure<KotlinAndroidProjectExtension> {
        jvmToolchain(JDK_TOOLCHAIN_VERSION)
        compilerOptions {
            jvmTarget.set(kotlinJvmTarget)
            allWarningsAsErrors.set(true)
        }
    }
    addCommonTestDependencies()
}

internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(JDK_TOOLCHAIN_VERSION)
        compilerOptions {
            jvmTarget.set(kotlinJvmTarget)
            allWarningsAsErrors.set(true)
        }
    }
    addCommonTestDependencies()
}

private fun Project.addCommonTestDependencies() {
    // Generated sources (Hilt, Room) make Gradle expect tests even in modules that have none yet.
    tasks.withType<Test>().configureEach {
        failOnNoDiscoveredTests.set(false)
        maxHeapSize = TEST_MAX_HEAP_SIZE
        maxParallelForks = 1
    }
    dependencies {
        add("testImplementation", libs.library("junit"))
        add("testImplementation", libs.library("assertk"))
        add("testImplementation", libs.library("kotlinx-coroutines-test"))
        add("testImplementation", libs.library("turbine"))
    }
}
