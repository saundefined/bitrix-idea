# AGENTS.md

Instructions for AI agents working in this repo.

## Project

Single-module Kotlin IntelliJ Platform plugin (`ru.panteleev.bitrix-idea`) for PhpStorm.
It creates Bitrix CMS module/template/component file sets and exposes the bundled Bitrix core
as a PHP library for navigation.

- Target platform: IntelliJ IDEA `2025.2.6.2` (build 252), configured in `build.gradle.kts`.
- Toolchain: JDK 21 (CI), Gradle wrapper 9.5, Kotlin 2.1.20, IntelliJ Platform Gradle Plugin 2.16.0.
- Kotlin package is `com.github.saundefined.bitrix_idea` (underscore). Icons live in the
  top-level `icons` package.

## Setup

The Bitrix core is a **git submodule**:

```sh
git submodule update --init --recursive
```

`src/main/resources/bitrix-idea-library` is the `qq-agency/bxApiDocs` submodule. It is bundled
into the plugin and registered via `com.jetbrains.php` `<libraryRoot>` in `src/main/resources/META-INF/plugin.xml`.
Do not edit or commit files inside it; update it by bumping the submodule pointer.

## Commands

```sh
./gradlew runIde        # launch sandbox IDE for manual testing
./gradlew buildPlugin   # build installable ZIP in build/distributions/
./gradlew check         # compile and run unit tests
./gradlew test          # run unit tests only
./gradlew test --tests 'com.github.saundefined.bitrix_idea.validation.ValidationTest'
./gradlew verifyPlugin  # JetBrains plugin verifier (slow, downloads IDE builds)
```

CI (`build.yml` calls the reusable `.github/workflows/build-and-test.yml`) validates the Gradle
wrapper, then runs `./gradlew buildPlugin check` and `./gradlew verifyPlugin`. Run these before
finishing a change. `build/`, `.gradle/`, `.intellijPlatform/`, `.kotlin/`, and `.idea/` are ignored
build/IDE artifacts. There are no separate lint/format/typecheck tasks; Kotlin compilation is the
type check. Unit tests live in `src/test/kotlin` and use JUnit 4.

`runIde` starts IntelliJ IDEA, not PhpStorm, so PHP-specific behavior needs manual testing in PhpStorm.
The Bitrix core is attached via the platform `AdditionalLibraryRootsProvider`, so the provider is
evaluated per project rather than globally.

## Architecture

`plugin.xml` is the wiring point: actions, file-template groups, settings, and the startup
notification are all registered there. A new action/extension requires a `plugin.xml` entry.

Creation flow: `CreateBitrixFileAction` resolves the project and target directory, then calls
`Create*Action.perform` → `Create*DialogWrapper` → J2EE file templates. Dialogs extend
`AbstractCreateDialogWrapper`, which owns the shared fields and runs `createFiles` in a write action.
`BitrixTemplateWriter` creates files from a private copy of the IDE default properties. The language
editor is shared via `ui/LocalizationTable`, which is also used by the settings page.

File templates are coupled in two places and must stay in sync:

1. Template file: `src/main/resources/fileTemplates/j2ee/<exact name>.ft`
   (names include spaces and leading dots, e.g. `Bitrix Module .settings.php.ft`).
2. Constant in `templates/BitrixTemplateNames.kt`, used by both the template group factories and
   the creation dialogs.

## Conventions and gotchas

- Комментарии в коде (KDoc, строчные комментарии и комментарии в шаблонах) пишутся на русском.
- User-visible dialog/notification text goes through `BitrixIdeaBundle.message("key")`;
  add keys to `src/main/resources/messages/BitrixIdeaBundle.properties`. Existing values use
  Unicode escapes (`\u0415...`) and are Russian. Action labels in `plugin.xml` are inline Russian.
- Settings are application-wide: `AppSettingsState` is an application service registered in
  `plugin.xml`, obtained via `AppSettingsState.getInstance()`, and persisted under the name
  `TestBitrixIdeaSettingsPlugin.xml`.
- The bundled Bitrix library is attached when the project-level `PHP > Frameworks > Bitrix` setting
  is enabled. This is implemented by `library/BitrixLibraryRootsProvider`; do not re-add a static
  `<libraryRoot>` to `plugin.xml`. The setting is persisted in `.idea/bitrixIdea.xml`; a startup
  notification offers to enable it when `bitrix` or `local` directories are found anywhere in the
  project.
- The ORM entity generator lives in `orm/` (`OrmEntityGenerator` is pure and unit-tested),
  `dialogs/CreateOrmEntityDialogWrapper`, and the `Bitrix ORM Entity.php.ft` template. It supports
  scalar fields, Enum/Json/Array, and Reference/OneToMany/ManyToMany relations. ManyToMany requires
  a mediator table name. Namespace inference treats `local/modules` and `bitrix/modules` the same:
  module code `vendor.module` -> `Vendor\Module`, `vendor` -> `Vendor`; the field remains editable.
- Validation rules are plain Kotlin objects in `validation/` (no platform dependencies) and are
  covered by `src/test/kotlin/.../validation/ValidationTest.kt`.
- Do not hand-edit `since-build`/`until-build` in `plugin.xml`; the Gradle plugin injects them
  from the target platform. Change the platform version in `build.gradle.kts` instead.

## Release

- Version lives in `gradle.properties` (`version=2026.1.0`).
- Release notes come from the `## [Unreleased]` section of `CHANGELOG.md`; add entries there.
- Pushing a tag matching `<version>` or `v<version>` triggers `.github/workflows/release.yml`.
  The workflow validates that the tag matches `gradle.properties`, then builds, verifies, and
  publishes a GitHub release from `build/distributions/*.zip`.
- When the supported PhpStorm line changes, also update the version table in `README.md`.
