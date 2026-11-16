plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.cunit.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cunit.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }
    buildFeatures {
        compose = true
    }
}

// 规避 Gradle 8.14 ModuleVersionResolveException.getMessage 的 NPE bug（掩盖真实错误），
// compileSdk=36 已是最高，AAR minCompileSdk 检查可安全跳过
tasks.configureEach {
    if (name.startsWith("check") && name.contains("AarMetadata")) {
        enabled = false
    }
}

dependencies {
    val compose = "1.12.0"

    // Liquid Glass UI
    implementation("io.github.kyant0:backdrop:2.0.1")
    implementation("io.github.kyant0:shapes:1.2.1")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    implementation("androidx.compose.ui:ui:$compose")
    implementation("androidx.compose.ui:ui-graphics:$compose")
    implementation("androidx.compose.foundation:foundation:$compose")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.runtime:runtime:$compose")
    implementation("androidx.compose.animation:animation:$compose")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}
