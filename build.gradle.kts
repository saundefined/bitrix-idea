plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2025.2.6.2")

        // PHP PSI/API нужен для навигации, автодополнения, инспекций и live-шаблонов.
        // Для офлайн-сборки укажите -PphpPluginPath=<каталог распакованного PHP-плагина>.
        val phpPluginPath = providers.gradleProperty("phpPluginPath").orNull
        if (phpPluginPath != null) {
            localPlugin(file(phpPluginPath))
        } else {
            plugins("com.jetbrains.php:252.28539.149")
        }
    }
}

intellijPlatform {
    // Инструментирование не требуется и требует загрузки ant-tasks из сети.
    instrumentCode.set(false)
    pluginVerification {
        freeArgs = listOf("-mute", "TemplateWordInPluginName")
    }
}