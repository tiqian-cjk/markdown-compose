pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "markdown-compose"

val tiqianCheckout = providers.gradleProperty("tiqianCheckout").orNull
    ?: System.getenv("TIQIAN_CHECKOUT")
    ?: "../Tiqian"
val tiqianSettings = file(tiqianCheckout).resolve("settings.gradle.kts")
if (tiqianSettings.isFile) {
    val composeProject = if (file(tiqianCheckout).resolve("frontend/compose").isDirectory) {
        ":frontend:compose"
    } else {
        ":tiqian-compose"
    }
    includeBuild(tiqianCheckout) {
        dependencySubstitution {
            substitute(module("org.tiqian:tiqian-compose")).using(project(composeProject))
        }
    }
}
