/**
 * ================================================================================================
 * WHAT IS IT:
 * Global Gradle settings definition file configuring multi-project hierarchy and repository sources.
 *
 * WHAT IT DOES:
 * 1. Configures plugin management repositories (Google Maven, Maven Central, Gradle Plugin Portal).
 * 2. Establishes dependency resolution management with `FAIL_ON_PROJECT_REPOS` mode to prevent
 *    rogue per-module repository definitions.
 * 3. Declares the root project name (`AutoDialer`) and includes application modules (`:app`).
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Upstream Orchestration: Gradle wrapper build initialization (`gradle/wrapper/gradle-wrapper.properties`).
 * - Downstream Projects: Application module `:app` (`app/build.gradle.kts`).
 * - Remote Repositories: Google Maven and Maven Central artifact repositories.
 * ================================================================================================
 */
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AutoDialer"
include(":app")
