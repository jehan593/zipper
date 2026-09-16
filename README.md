# Zipper

[![Build APK](https://github.com/jehan593/zipper/actions/workflows/build-apk.yml/badge.svg)](https://github.com/jehan593/zipper/actions/workflows/build-apk.yml)
[![Latest release](https://img.shields.io/github/v/release/jehan593/zipper)](https://github.com/jehan593/zipper/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

An offline Android app for creating, previewing, and extracting archives.

> FYI: This project is fully vibe coded.

## Features

- Create ZIP, 7z, TAR, and TAR.GZ archives from files and folders.
- Add an optional password to ZIP and 7z archives.
- Preview and extract ZIP, 7z, RAR, TAR, and TAR.GZ archives.
- Open a supported archive with Zipper from a file manager to browse its folders in a popup.
- Choose files and save locations with Android's file picker. No storage permissions needed.
- Use light and dark themes with the Nord palette and Martian Mono font.

Passwords protect file contents, but file names remain visible. RAR archives can be
previewed and extracted, but cannot be created.

## Install

Download the latest APK from [Releases](https://github.com/jehan593/zipper/releases/latest).
Requires Android 8.0 or later.

## Build from source

Install JDK 17 and Android SDK 35. Set the SDK location in a `local.properties` file:

```properties
sdk.dir=/path/to/android-sdk
```

Build a release APK:

```sh
./gradlew assembleRelease
```

On Windows, use `.\gradlew.bat assembleRelease`. The APK is saved to
`app/build/outputs/apk/release/app-release.apk`.
Use `assembleDebug` for a debug build.

## Development

Built with Kotlin, Jetpack Compose, and Material 3. Archive support comes from
zip4j, Apache Commons Compress, XZ, and junrar. Files are accessed through
Android's file picker.

See [CLAUDE.md](CLAUDE.md) for architecture and development notes.

## License

The app is licensed under [MIT](LICENSE). The bundled font uses the
[SIL Open Font License 1.1](app/licenses/MARTIAN_MONO_LICENSE.txt).
The junrar library uses the UnRAR license; see its
[Maven Central page](https://central.sonatype.com/artifact/com.github.junrar/junrar) for details.
