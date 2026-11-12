/**
 * ================================================================================================
 * WHAT IS IT:
 * Primary module-level Gradle build configuration script for the `:app` Android application module.
 *
 * WHAT IT DOES:
 * 1. Applies core compilation plugins: Android Application, Kotlin Android, Jetpack Compose Compiler,
 *    and Kotlin Symbol Processing (KSP).
 * 2. Defines Android build parameters: Namespace (`com.autodialer.app`), Compile SDK 35, Min SDK 26,
 *    Target SDK 35, and semantic versioning (1.0.0 / versionCode 1).
 * 3. Configures dynamic release signing: Uses `upload-keystore.jks` when present in root or module directory,
 *    falling back seamlessly to debug signing when building from an extracted standalone codebase without keys.
 * 4. Declares runtime and test dependencies from Version Catalog (`libs.versions.toml`) covering
 *    Jetpack Compose Material 3, AndroidX Lifecycle, Room Database (KSP), Coroutines, and JUnit 4.
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Upstream Catalog: `gradle/libs.versions.toml`
 * - Root Config: `build.gradle.kts` and `settings.gradle.kts`
 * - Source Packages: `com.autodialer.app.*` (UI, Services, Database, Domain Models)
 * - Keystore Artifacts: `upload-keystore.jks` in root directory
 * ================================================================================================
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.autodialer.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.autodialer.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    /**
     * Flexible signing configuration:
     * Attempts to resolve 'upload-keystore.jks' from the project root or module root.
     * If keystore file is omitted (e.g. standalone codebase extracted from APK),
     * it initializes with debug signing so building always succeeds out-of-the-box.
     */
    signingConfigs {
        create("release") {
            val rootKeystore = file("../upload-keystore.jks")
            val localKeystore = file("upload-keystore.jks")
            val targetKeystore = if (rootKeystore.exists()) rootKeystore else if (localKeystore.exists()) localKeystore else null

            if (targetKeystore != null) {
                storeFile = targetKeystore
                storePassword = "autodialer123"
                keyAlias = "upload"
                keyPassword = "autodialer123"
            } else {
                initWith(signingConfigs.getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        checkReleaseBuilds = true
        abortOnError = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // AndroidX & Kotlin Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose & Material 3 Expressive Design System
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room Database SQLite Engine (KSP Code Generation)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Kotlin Coroutines & JSON Serialization
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.gson)

    // Unit Testing & Tooling
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.ui.tooling)
}
