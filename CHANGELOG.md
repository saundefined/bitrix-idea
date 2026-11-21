# CHANGELOG

## [Unreleased]

### Added

- Autocompletion for `$arParams['…']` with parameter type, default value and standard keys
- Quick fixes: create missing component parameter, template, template directory, and lang key
- Component parameter editor with type, default value and PARENT group
- Preview before applying generators and undoable generator writes (`WriteCommandAction`)
- Template rename also updates `COMPONENT_TEMPLATE` in `IncludeComponent` parameters
- Module options page generator (`options.php` + lang)
- Admin page and `admin/menu.php` generator
- REST controller and `install/routes.php` generator
- Console command generator
- Component `class.php` skeleton generator
- Completion of event names and controller namespaces in `.settings.php`
- Event handler signature hints in documentation and completion
- Version-aware legacy API inspection and module version mismatch inspection
- Lang placeholder validation and required component parameter inspection
- Goto Related for components and lang translations
- Find Usages/Rename for components in the Bitrix tool window
- Tool window grouping by module or vendor
- Component/template Find Usages scoped to the owning component
- Duplicate lang keys across files inspection
- Tool window expansion state, “only enabled modules” toggle, diagnostics copy and core path opening
- Structured `.settings.php` editor with preview and section-preserving updates (comments outside sections are kept)
- Component parameter generator now preserves existing lang translations
- Quick fix creating an event handler class from `.settings.php`
- Bitrix UI generators: modern grid page, grid/filter/form configs, and admin list/edit pages
- Component refactoring: extract from selection, move to `local`, convert to complex component, copy template to local site template
- Component template chain dialog with effective template and parent navigation
- Bitrix Health tool window with structural/lang/version diagnostics, filtering, ignore list, and its own setting
- Inlay hints with the effective `IncludeComponent` template
- Search Everywhere for lang messages and `.settings.php` classes/events
- Translation table with inline editing and CSV export for lang files
- `.settings.php` structure dialog and section-preserving edits
- Custom lang definition index for faster navigation and Find Usages
- Project Health now also runs the plugin PHP inspections
- Moving a template to a local site template can delete the source template
- Plugin verifier runs in CI for the configured PhpStorm versions
- Faster lang/component scans with text prefilters
- Core component template override generator (`local/templates/<site>/components/...`)
- Component parameter generator with `.parameters.php` and lang files
- Event handler and agent generators with `.settings.php` registration
- Service/interface and AJAX action generators
- Component/template rename with updates to all `IncludeComponent` calls
- Navigation from `.settings.php` events, `IncludeComponent` metadata, and `$arParams['KEY']` to declarations
- Ctrl+Q documentation for `.settings.php`, components, templates, modules, lang messages, and parameters
- Search Everywhere support for Bitrix components, templates, and modules
- Gutter icon opening the effective template of `IncludeComponent`
- Language message settings: preferred language, folding switch, multiple translations in Ctrl+Q
- Unused/duplicate lang message and unknown/unused component parameter inspections
- Project diagnostics, core source selection, component/template indexing filters, and “Переиндексировать Bitrix”
- Tool window lang branch, disabled module styling, copy/compare/create-template context actions
- PSI tests for lang navigation, folding, `.settings.php` registration, and component parameters (`-PpsiTests=true`)
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

- Build infrastructure updated to IntelliJ Platform Plugin Template `2.7.0`: IntelliJ Platform Gradle Plugin `2.20.1`, Kotlin `2.4.21`, Gradle Wrapper `9.9.2`
- CI workflows use current GitHub Actions releases (`checkout@v7`, `setup-java@v6`, `upload-artifact@v7`, `download-artifact@v8`, `free-disk-space@v2.0.0`); the plugin ZIP is uploaded without re-archiving
- Vendor and localization settings are now edited in `PHP > Frameworks > Bitrix` instead of a separate Tools page
- PHP insight is loaded through an optional PHP plugin dependency, so the plugin also installs in PhpStorm Lite
- Refactored dialogs, actions, file-template handling, and settings code
- Kotlin default methods are compiled without compatibility bridges (`jvm-default=no-compatibility`), so the plugin verifier no longer reports compiler-generated internal/deprecated API usages

### Fixed

- Empty names and descriptions are rejected when creating modules, templates, and components
- URL validation rejects addresses without a host
- Localization changes in settings are applied only after confirmation
- Bitrix tool window no longer risks `MissingResourceException` for the component templates node (`toolwindow.template.tree` key added)
- Removed deprecated and internal platform API usages: `Project.getBaseDir()`, `InlayHintsSink.addInlineElement`, and `InspectionManager.defaultProcessFile`
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