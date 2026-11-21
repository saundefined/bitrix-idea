import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "Bitrix Idea"

pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.4.21"
        id("org.jetbrains.changelog") version "2.5.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.intellij.platform.settings") version "2.20.1"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()

        intellijPlatform {
            defaultRepositories()
        }
    }
}