# Zipper

[![Build APK](https://github.com/jehan593/zipper/actions/workflows/build-apk.yml/badge.svg)](https://github.com/jehan593/zipper/actions/workflows/build-apk.yml)
[![Latest release](https://img.shields.io/github/v/release/jehan593/zipper)](https://github.com/jehan593/zipper/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Zipper is a fully offline Android archive manager: create password-protected ZIP/7z archives (or
plain TAR/TAR.GZ) from files and folders you pick, extract ZIP/7z/RAR/TAR/TAR.GZ archives to a
folder you choose, and preview any archive's contents in a popup the moment you tap it in a file
manager.

## Features

- **Create Archive** — pick any mix of files and folders, choose ZIP, 7-Zip, TAR, or TAR.GZ,
  optionally add an AES-256 password (ZIP and 7z only — TAR/TAR.GZ have no encryption), then save
  wherever you like.
- **Extract Archive** — pick a ZIP, 7z, RAR, TAR, or TAR.GZ file and it opens straight into the
  same look-inside popup used for tap-to-preview below: enter its password if it has one, then
  extract to a folder you choose.
- **Tap-to-preview** — tapping an archive anywhere on the device (file manager, downloads, email
  attachment) opens a small popup with a folder-drill-down look inside, plus an Extract button —
  no need to open the full app.
- **Nord theme** — dark/light color schemes built on the [Nord](https://www.nordtheme.com/)
  palette, with Martian Mono Nerd Font throughout.

Passwords protect file *content* only, never the file list — ZIP never encrypts filenames
regardless of library (a format property), and while 7z can, the Java library this app builds on
doesn't implement that half of the spec.

RAR is extract-only: no free/open-source library can *write* the proprietary RAR format (see
`CLAUDE.md` for details), so Create only ever offers formats Zipper can also read back.

## Install

Grab the latest APK from [Releases](https://github.com/jehan593/zipper/releases/latest).

## Building from source

```sh
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk (minified, resource-shrunk)
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
```

Requires an Android SDK referenced via `local.properties` (`sdk.dir=...`). `compileSdk` 35,
`minSdk` 26.

## Tech stack

Kotlin, Jetpack Compose, Material 3. No database, no DI framework — every screen is a self-contained
one-shot operation. Archive formats via zip4j (ZIP), Apache Commons Compress + XZ (7z, TAR, TAR.GZ),
and junrar (RAR, read-only). Every file operation goes through the Storage Access Framework — no
storage permissions anywhere. See `CLAUDE.md` for the full architecture writeup.

## License

MIT — see [`LICENSE`](LICENSE). The bundled Martian Mono Nerd Font is licensed separately under
the SIL Open Font License 1.1 — see
[`app/licenses/MARTIAN_MONO_LICENSE.txt`](app/licenses/MARTIAN_MONO_LICENSE.txt). junrar (RAR
extraction) ships under its own "UnRAR license", not MIT/Apache — see
[Maven Central](https://central.sonatype.com/artifact/com.github.junrar/junrar) for terms.
