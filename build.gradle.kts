import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform") version "2.3.20"
    id("com.android.kotlin.multiplatform.library") version "9.3.1"
    id("org.jetbrains.compose") version "1.11.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
}

group = "org.tiqian"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvmToolchain(25)

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
        }
    }

    android {
        namespace = "org.tiqian.markdown"
        compileSdk = 37
        minSdk = 27
        androidResources.enable = true
    }

    sourceSets {
        commonMain.dependencies {
            api("org.tiqian:tiqian-compose:0.1.0-SNAPSHOT")
            api(compose.runtime)
            api(compose.ui)
            api(compose.components.resources)
            implementation(compose.foundation)
            implementation("org.tiqian.math:math-compose:0.1.0-SNAPSHOT")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.resources {
    packageOfResClass = "org.tiqian.markdown.generated.resources"
}
