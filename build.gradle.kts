import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform") version "2.3.20"
    id("com.android.kotlin.multiplatform.library") version "9.2.1"
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
        compileSdk = 36
        minSdk = 31
    }

    sourceSets {
        commonMain.dependencies {
            api("org.tiqian:tiqian-compose:0.1.0-SNAPSHOT")
            api("io.github.zly2006:markdown-parser:0.0.1-alpha.12")
            api(compose.runtime)
            api(compose.ui)
            implementation(compose.foundation)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}
