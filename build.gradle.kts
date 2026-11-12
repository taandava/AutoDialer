/**
 * ================================================================================================
 * WHAT IS IT:
 * Root-level Gradle build configuration script for the Auto Dialer Android project.
 *
 * WHAT IT DOES:
 * 1. Declares top-level Gradle plugins managed across all submodules without applying them directly.
 * 2. Coordinates Android Application, Kotlin Android, Jetpack Compose Compiler, and KSP plugin versions.
 * 3. Enforces version synchronization via Gradle Version Catalogs (`gradle/libs.versions.toml`).
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Upstream Catalog: `gradle/libs.versions.toml` (central dependency repository)
 * - Downstream Modules: `:app` module ([app/build.gradle.kts])
 * - Build Plugins: Android Gradle Plugin (AGP), Kotlin Gradle Plugin, KSP, Compose Compiler
 * ================================================================================================
 */
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
