# Bitrix Idea

[![Version](http://phpstorm.espend.de/badge/14703/version)](https://plugins.jetbrains.com/plugin/14703)
[![Downloads](http://phpstorm.espend.de/badge/14703/downloads)](https://plugins.jetbrains.com/plugin/14703)
[![Rating](https://img.shields.io/jetbrains/plugin/r/rating/14703-bitrix-idea?style=flat-square)](https://plugins.jetbrains.com/plugin/14703)
![Build](https://github.com/saundefined/bitrix-idea/actions/workflows/build.yml/badge.svg)
[![License](https://img.shields.io/github/license/saundefined/bitrix-idea?style=flat-square)](https://plugins.jetbrains.com/plugin/14703)

Plugin for [PhpStorm](https://www.jetbrains.com/phpstorm) to help
the developer navigate the Bitrix CMS core source code.

| Key        | Value                                                  |
|------------|--------------------------------------------------------|
| Plugin url | https://plugins.jetbrains.com/plugin/14703-bitrix-idea |
| Id         | ru.panteleev.bitrix-idea                               |
| Changelog  | [CHANGELOG](CHANGELOG.md)                              |

## Support us

[<img src="https://sergeypanteleev.storage.yandexcloud.net/ad/saundefined-bitrix-idea.jpg"/>](https://sergeypanteleev.com/ad/bitrix-idea)

## Installation

To install, go to Settings > Plugins and search for "Bitrix Idea".

## Supported IDEs

The plugin can be installed on following IntelliJ-based:

* PhpStorm 2025.2+
* PhpStorm Lite 2025.2+ (PHP insight is loaded only when the PHP plugin is available)

## Features

### Navigation and insight

- Ctrl+Click from `IncludeComponent` to component files, `.description.php`, `.parameters.php`, and the selected template
- Navigation from `.settings.php` event handlers to classes/methods
- Navigation from `$arParams['KEY']` to the parameter declaration in `.parameters.php`
- Ctrl+Q for components, templates, modules, lang messages, `.settings.php`, and component parameters
- Gutter icons for component files, templates, modules, lang messages, and the effective `IncludeComponent` template
- Search Everywhere (Go to Symbol) for Bitrix components, templates, and modules
- Completion for component IDs, module IDs, template names, lang keys, events, and agent classes
- Folding of `Loc::getMessage('KEY')` to the selected language phrase
- Inspections for missing/duplicate/unused lang messages, unknown modules/components/parameters, missing templates/lang files, unregistered settings classes, and event handlers

### Generators and refactoring

- Module, simple and complex component, component template, site template, D7 class, ORM entity, service/interface, event handler, and agent generators
- Core component template override into `local/templates/<site>/components/...`
- Component parameter generator (`.parameters.php` and lang files)
- AJAX action generator for D7 controllers
- Rename of component/template IDs with updates to every `IncludeComponent` call
- Rename of lang keys across all languages and usages

### Settings and tool window

- `PHP > Frameworks > Bitrix` settings page: framework switch, core source (bundled library or project `bitrix`), Bitrix root, source roots, module/component/template indexing filters, default lang language, folding switch, and project diagnostics
- “Переиндексировать Bitrix” button and tool window action
- Bitrix tool window with modules (disabled modules are grayed out), components grouped by vendor, site templates, and lang files
- Tool window filter, loading indicator, context menu actions (copy path/name, compare with core `.default`, create component template, open settings)

## Development

```sh
./gradlew buildPlugin check
./gradlew verifyPlugin
./gradlew test -PpsiTests=true
./gradlew buildPlugin -PplatformVersion=2025.1.5 -PphpPluginVersion=<php-plugin-build>
```

The same `platformVersion` and `phpPluginVersion` inputs are available in the manual
`Build` GitHub Actions workflow, so compatibility with another PhpStorm line can be checked
without changing the repository.

## Release versions schema

| Plugin version number | Since Version | Until Version |
|-----------------------|---------------|---------------|
| v2026.1.x             | 2025.2        |               |
| v2025.1.x             | 2024.2        | 2025.2        |
| v2024.3.x             | 2024.1        | 2024.3        |
| v2024.2.x             | 2023.2        | 2024.2        |
| v2024.1.x             | 2023.1        | 2024.1        |
| v2023.7.x             | 2022.3        | 2023.3        |
| v2023.6.x             | 2022.3        | 2023.3        |
| v2023.5.x             | 2022.2        | 2023.2        |
| v2023.4.x             | 2022.2        | 2023.2        |
| v2023.3.x             | 2021.3        | 2023.2        |
| v2023.2.x             | 2021.3        | 2023.1        |
| v2023.1.x             | 2021.3        | 2023.1        |

## License

The plugin is distributed under MIT.
For full license terms, see [LICENCE](LICENCE.md).

## Screenshots

![bitrix_idea phpstorm](https://plugins.jetbrains.com/files/14703/screenshot_22644.png)