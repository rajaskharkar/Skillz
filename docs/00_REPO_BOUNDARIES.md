# Repository Boundaries

## 1. Purpose

This repository uses a platform-separated monorepo layout so Android and iOS work can live in one repository while each platform remains independently openable, buildable, runnable, and testable from its own project folder.

The goal is to replicate the existing Android app on iOS without turning either platform into a dependency of the other. Android Studio should open the Android project directly from `android/`, and the future Xcode project should open directly from `ios/`. Root-level documentation may coordinate planning and parity work, but it must not become part of either platform's build or runtime dependency graph.

## 2. Current Repository Layout

The intended repository layout is:

```text
Skillz/
  android/   Android project, opened directly in Android Studio
  ios/       future iOS project, opened directly in Xcode
  docs/      shared reference documentation only
```

The current observed repository structure contains the Android project under `android/`. A future `ios/` folder is expected to contain the iOS project, but no iOS project should be created until an explicit future task asks for it. The `docs/` folder is for shared reference documentation only.

Key Android files and directories currently observed include:

- `android/settings.gradle.kts`, the Android Gradle settings file.
- `android/build.gradle.kts`, the Android top-level Gradle build file.
- `android/app/build.gradle.kts`, the Android app module Gradle build file.
- `android/app/src/`, the Android app source, resource, and test tree.
- `android/gradle/`, the Android Gradle version catalog and wrapper-related configuration area.
- `android/gradlew` and `android/gradlew.bat`, the Android Gradle wrapper launchers.

## 3. Project Boundary Rules

- Android build and runtime dependencies must stay inside `android/`.
- iOS build and runtime dependencies must stay inside `ios/`.
- Shared docs may live in `docs/`.
- Shared docs are reference-only and must not be imported, copied, generated from, or referenced by platform build systems.
- Future shared test fixtures, assets, fonts, audio, generated files, or other implementation inputs must not become required runtime or build dependencies outside a platform folder.
- If both platforms need the same asset, fixture, font, audio file, schema, generated output, or similar implementation input, each platform should receive its own local copy inside its own folder.
- Root-level files and folders must not be required to build, run, or test Android or iOS unless a later explicit architecture decision changes this rule.

## 4. Android Project Rules

- Android Studio should open `android/` directly.
- The Gradle root for Android should remain inside `android/`.
- Android source, resources, fonts, drawables, schemas, tests, Gradle files, and build scripts should remain under `android/`.
- Android must not depend on `../ios`, `../docs`, or future root-level shared folders for build behavior, test behavior, runtime behavior, asset packaging, code generation, or resource generation.
- Android-specific reference notes may point readers to `docs/`, but Android build scripts and runtime code must not require files from `docs/`.

## 5. iOS Project Rules

- The future Xcode project should live under `ios/`.
- Xcode should open the project from `ios/` directly.
- iOS source, resources, fonts, audio, asset catalogs, tests, build scripts, Swift package dependencies, CocoaPods configuration, generated files, and other package dependencies should remain under `ios/`.
- iOS must not depend on `../android`, `../docs`, or future root-level shared folders for build behavior, test behavior, runtime behavior, asset packaging, code generation, or resource generation.
- iOS may use `docs/` as human-readable planning reference, but the Xcode project, package manifests, scripts, and runtime code must not require files from `docs/`.

## 6. Allowed Root-Level Content

Allowed root-level content includes:

- `docs/` for planning, specs, audits, parity checklists, migration notes, and other shared reference documentation.
- Root-level files such as `README.md`, repository notes, license files, or general project metadata if needed.
- Repository management files that do not become build or runtime dependencies of either platform.

No root-level runtime dependency may be required by Android or iOS. No root-level build dependency may be required by Android or iOS unless a later explicit architecture decision changes the repository boundary model.

## 7. Disallowed Patterns

The following patterns are disallowed under the current repository boundary rules:

- iOS referencing Android implementation files, such as `../android/app/src/main/res/font/...`.
- Android referencing iOS implementation files, such as `../ios/...`.
- Xcode build phases copying files from `../android`.
- Gradle scripts depending on files from `../ios`.
- Either platform importing or copying required build inputs from `../docs`.
- Shared runtime code outside platform folders unless a later explicit architecture decision is made.
- Assets living only at the repository root while required by one or both apps.
- Fonts, audio, drawables, images, schemas, fixtures, generated sources, or generated resources stored outside platform folders when required for platform build, test, or runtime behavior.
- Build scripts that assume the repository root is the platform project root.

## 8. Future Codex Task Requirements

Every future Codex implementation task should preserve these repository boundary rules and report:

- Android files inspected.
- iOS files created or changed.
- Whether Android remains openable from `android/`.
- Whether iOS remains openable from `ios/`.
- Whether any platform dependency was introduced outside its own folder.

If a future task requires an exception, the task should first document the proposed architecture decision and explicitly identify why the exception is necessary.

## 9. Acceptance Criteria

- Only `docs/00_REPO_BOUNDARIES.md` is created or changed.
- Android source code is untouched.
- No iOS project is created yet.
- No build files are changed.
- The document clearly states that `docs/` is reference-only.
- The document clearly states that Android and iOS must be independently openable from their own folders.
