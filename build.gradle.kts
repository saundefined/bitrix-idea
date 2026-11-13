import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

// PSI-тесты требуют загрузки tests-bootstrap. В офлайн-окружении их можно отключить:
// ./gradlew test -PpsiTests=true
val psiTests = providers.gradleProperty("psiTests").isPresent
val platformVersion = providers.gradleProperty("platformVersion").orNull
    ?.takeIf { it.isNotBlank() } ?: "2025.2.6.2"
val phpPluginVersion = providers.gradleProperty("phpPluginVersion").orNull
    ?.takeIf { it.isNotBlank() } ?: "252.28539.149"

dependencies {
    testImplementation("junit:junit:4.13.2")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea(platformVersion)
        if (psiTests) {
            testFramework(TestFrameworkType.Platform)
        }

        // PHP PSI/API нужен для навигации, автодополнения, инспекций и live-шаблонов.
        // Для офлайн-сборки укажите -PphpPluginPath=<каталог распакованного PHP-плагина>.
        val phpPluginPath = providers.gradleProperty("phpPluginPath").orNull
        if (phpPluginPath != null) {
            localPlugin(file(phpPluginPath))
        } else {
            plugins("com.jetbrains.php:$phpPluginVersion")
        }
    }
}

sourceSets {
    test {
        if (!psiTests) {
            kotlin.exclude("com/github/saundefined/bitrix_idea/php/BitrixPsiTest.kt")
        }
    }
}

intellijPlatform {
    // Инструментирование не требуется и требует загрузки ant-tasks из сети.
    instrumentCode.set(false)
    pluginVerification {
        ides {
            create(IntelliJPlatformType.PhpStorm, platformVersion)
        }
        freeArgs = listOf("-mute", "TemplateWordInPluginName")
    }
}