# CHANGELOG

## [Unreleased]

### Added

- Bitrix ORM entity generator reworked to modern `configure*()` syntax with validators, column mapping, default values, expressions, Crypto/Secret/Array fields, and Reference/OneToMany/ManyToMany relations
- ORM full stack generation: `Table`, `Collection`, and `Object` classes; custom classes extend `EO_*`; optional `meta/orm.php` annotations
- ORM installer generation via `install/orm.php` and `createDbTable()` / mediator entities; raw `install.sql` remains optional
- Project-level `PHP > Frameworks > Bitrix` setting; the bundled Bitrix core is attached when it is enabled
- Notification offering to enable Bitrix Framework support when `bitrix` or `local` directories are detected
- Navigation to component classes (`class.php` / `component.php`), component templates, modules, and lang messages
- Completion for component IDs, module IDs, template names, lang keys, events, and agent classes
- Gutter icons for `IncludeComponent`, `IncludeModule`, and `Loc::getMessage`
- Inspections for missing lang messages, unknown modules/components, legacy `CModule`/`GetMessage` calls, and `ru`/`en` lang sync
- Bitrix live templates for components, modules, events, agents, cache, ORM, and localization
- Configurable Bitrix root directory when `bitrix` and `local` are not in the project root
- Automatic source roots for modules and a module indexing filter (all modules are enabled by default)
- Module indexing settings include modules from the bundled `bitrix-idea-library`; disabled modules are excluded from indexing and completion
- Full module scaffold options: install/uninstall steps, SQL files, admin menu, options page, and `.settings.php` sections
- Module wizard can create a component in `install/components` and generate `InstallFiles()` / `UnInstallFiles()`
- Module installer now follows the documented admin flow with `IncludeAdminFile()` result screens
- Component wizard uses separate vendor/name fields and creates `local/components/<vendor>/<name>`
- Component scaffold now includes `script.js`, `style.css`, optional `result_modifier.php` / `component_epilog.php`, guards, and a cache-ready `class.php`
- Bitrix tool window with modules, components, and site templates

### Changed

- Vendor and localization settings are now edited in `PHP > Frameworks > Bitrix` instead of a separate Tools page
- PHP insight is loaded through an optional PHP plugin dependency, so the plugin also installs in PhpStorm Lite
- Refactored dialogs, actions, file-template handling, and settings code

### Fixed

- Empty names and descriptions are rejected when creating modules, templates, and components
- URL validation rejects addresses without a host
- Localization changes in settings are applied only after confirmation
- CI refactoring

## [2026.1.0]

### Changed

- Updated Bitrix CMS core
- Update IntelliJ Platform 2.6.0
- Update IntelliJ Platform 2.4.1

## [2025.1.0]

### Changed

- Update IntelliJ Platform 2.4.0
- Update IntelliJ Platform 2.3.1
- Update IntelliJ Platform 2.2.0

## [2025.1.0]

### Changed

- Update IntelliJ Platform 2.1.0

## [2024.3.0]

### Changed

- Updated Bitrix CMS core
- Update IntelliJ Platform 1.14.1
- Update IntelliJ Platform 1.14.2
- Update IntelliJ Platform 2.0.0
- Update IntelliJ Platform 2.0.1
- Update IntelliJ Platform 2.0.2
- Support for PhpStorm 2024.3

## [2024.2.0]

### Added

- Creating Bitrix-related .gitignore file

### Changed

- Updated Bitrix CMS core
- Update IntelliJ Platform 1.13.0
- Update IntelliJ Platform 1.14.0
- Support for PhpStorm 2024.2 EAP
- Fix icons

## [2024.1.0]

### Changed

- Updated Bitrix CMS core
- Support for PhpStorm 2024.1 EAP 
- Update IntelliJ Platform 1.12.0

## [2023.7.0]

### Changed

- Updated Bitrix CMS core
- Update IntelliJ Platform 1.11.3

## [2023.6.0]

### Changed

- Update IntelliJ Platform 1.10.0
- Update IntelliJ Platform 1.11.0
- Update IntelliJ Platform 1.11.2
- Updated Bitrix CMS core

## [2023.5.0]

### Added

- Module creating form
- Template creating form
- Simple component creating form
- Add Bitrix Idea settings
- Add localizations table to settings and create entity forms

### Changed

- Add "Don't show again" action to startup notification
- Update IntelliJ Platform 1.8.0
- Update IntelliJ Platform 1.9.0

## [2023.4.0]

### Changed

- Updated Plugin Core
- Updated Bitrix CMS core

## [2023.3.0]

### Changed

- Updated Plugin Core
- Updated Bitrix CMS core

## [2023.2.0]

### Changed

- Updated Plugin Core
- Updated Bitrix CMS core

## [2023.1.0]

### Changed

- Updated Plugin Core
- Updated Bitrix CMS core