# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Zipper: a fully offline Android archive manager. Nord color palette, Martian Mono Nerd Font,
Jetpack Compose UI (matches ownscreen/noter/linker's visual identity — see sibling repos at
`../ownscreen`, `../noter`, `../linker`). Package `com.zipper.app`, minSdk 26. No
`<uses-permission>` at all — every file the app touches (sources to archive, the archive being
read, the destination folder) is a `content://` Uri obtained through the Storage Access Framework,
never a raw filesystem path.

**Extract Archive has no screen of its own.** Home has two actions: Create Archive (its own
screen) and Extract Archive, which just launches a file picker and then shows the *same*
folder-drill-down popup (`ui/preview/ArchivePreviewScreen.kt`) used when an archive is tapped
elsewhere on the device (`ArchivePreviewActivity`, translucent, fires from the manifest's intent
filters) — one look-inside-and-extract UI serves both entry points instead of two nearly-identical
ones. Both entry points go through `ui/preview/ArchivePreviewHost.kt`, which wires up the
ViewModel and destination-folder launcher once and is shared rather than duplicated between
`MainActivity` (in-app overlay) and `ArchivePreviewActivity` (translucent popup); see
"Dialog, not Box" below for how the same screen composable works in both places.

## Commands

```sh
./gradlew assembleRelease     # build app/build/outputs/apk/release/app-release.apk (R8-minified, resource-shrunk — what CI ships)
./gradlew assembleDebug       # build app/build/outputs/apk/debug/app-debug.apk (local iteration only)
./gradlew build               # full build incl. lint/checks
```

- No test suite exists in this repo currently.
- `local.properties` needs `sdk.dir` pointing at an Android SDK (Windows: use forward slashes with
  an escaped drive colon, e.g. `sdk.dir=C\:/Users/jehan/android-sdk`).
- `versionCode`/`versionName` are overridable via `-PappVersionCode=`/`-PappVersionName=` — CI
  (`.github/workflows/build-apk.yml`) passes `github.run_number` so every push gets a strictly
  increasing versionCode and its own tag/release rather than overwriting one shared release.
- `app/debug.keystore` is the same committed keystore file used by noter/linker/ownscreen — reused
  as-is rather than generated fresh, since signing-key *identity* only matters for in-place updates
  of the same app (matched by `applicationId`), never across different apps. The `release` build
  type reuses it too (see `app/build.gradle.kts`) so Obtainium in-place updates keep working even
  though the shipped variant is release, not debug.
- Release is minified (`isMinifyEnabled`), resource-shrunk (`isShrinkResources`), and ABI-filtered
  to `arm64-v8a`/`armeabi-v7a`. `app/proguard-rules.pro` keeps all four archive libraries whole
  (they do their own reflection/ServiceLoader work internally) and `-dontwarn`s Commons Compress's
  optional zstd/brotli/pack200(ASM) codec classes — none of those extra libraries are on the
  classpath since this app only ever touches zip/7z/tar/gzip, but R8 needs telling that's expected
  rather than a real missing-class bug.
- Build the APK and hand it off rather than installing to an emulator/adb yourself — the user
  tests on their own device.

## Architecture

**No DI framework, no database.** Unlike noter/linker there's no `AppContainer`/repository layer —
every screen is a self-contained one-shot operation (pick sources, pick a destination, run, done)
with nothing to share across screens or persist between launches. ViewModels are plain
`ViewModel()` subclasses constructed via the default `viewModel()` factory.

**Format support matrix** (`archive/ArchiveFormat.kt`): ZIP and 7z support create + extract +
AES-256 password; TAR and TAR.GZ support create + extract but never a password (neither format has
an encryption concept — `archive/readers/TarReader.kt` and `archive/writers/TarWriter.kt` both take
a `gzip: Boolean` and share the same code for both, differing only in whether a
Gzip(De)compressor stream wraps the tar stream); RAR is **extract-only** — no free/open-source
library can *write* the proprietary RAR format, so it's structurally impossible to add RAR to
Create no matter what library is used. junrar (RAR reading) ships under a custom "UnRAR license"
rather than Apache/MIT/BSD like the other three — permits extraction freely but forbids using it to
build a RAR-compatible archiver. 7z's password support was initially assumed impossible (Commons
Compress was thought read-only for encryption) but `SevenZOutputFile(File, char[])` has supported
AES-256 writes since Commons Compress 1.23 — same password flow as ZIP.

**Password protection is content-only, never filenames.** Neither format hides the file list: ZIP's
central directory stores filenames unencrypted regardless of AES content encryption (a ZIP format
property, not a library limitation — no zip tool encrypts filenames in a standard ZIP). 7z *can*
hide filenames too (7-Zip's own "Encrypt file names" option), but Commons Compress's
`SevenZOutputFile` only ever wraps entry *content* in `AES256Options` — its header-writing code
(`writeFilesInfo`/`writeFileNames`) never references encryption at all, so that option isn't
something this app can expose without implementing 7z header encryption from the byte level
ourselves, which is out of scope.

**SAF staging, everywhere** (`archive/saf/`): zip4j, Commons Compress, and junrar all need real
`java.io.File`/`SeekableByteChannel` access — none can read or write a `content://` Uri directly.
`SafFileCollector.collect()` copies every picked file/folder into a temp `cacheDir` staging
directory before Create hands it to a writer; `StagedArchive.stage()` copies a picked/tapped
archive Uri into a staging file before Extract/Preview hands it to a reader.
`SafExtractionTarget` is the reverse direction — it turns an archive's internal `/`-separated entry
paths into real nested `DocumentFile`s under a destination tree Uri, creating each folder level
on demand and caching them by path so a thousand-entry archive doesn't re-walk the tree per file.
Every staging run lives under `stagingDir()` (`cacheDir/zipper_staging/<timestamp>/`), deleted by
the caller when its operation finishes and wiped wholesale on every cold start
(`ZipperApplication`) in case a crash left one behind.

**Password detection is a probe, not a flag.** None of the three encryptable formats validate a
password at open time — zip4j/Commons Compress/junrar all only throw once you actually try to
decompress an entry. Each reader's `open()` (`archive/readers/*.kt`) therefore decompresses one
small entry immediately after opening: if that throws with no password given, it's
`PasswordRequired`; if it throws with a password given, it's `WrongPassword`. This surfaces bad
passwords immediately in the UI instead of failing partway through a real extraction.

**Folder drill-down is synthesized, not read from the archive.** Many archives (most zips made by
non-Android tools included) never emit an explicit directory entry for every folder — only leaf
files with slash-separated paths. `archive/ArchiveTree.kt`'s `childrenAt()` builds the popup's
folder listing by grouping entries on their next path segment relative to the current folder,
falling back to `ArchiveEntryInfo.isDirectory` only to skip an explicit entry for a folder whose
children are already being listed some other way.

**Two manifest intent-filter blocks for one activity**
(`ArchivePreviewActivity`, see `AndroidManifest.xml`): archive mime-types aren't reported
consistently by whatever app the file was tapped from. `content://` sources (most file managers,
Downloads, email attachments) mostly set a correct mimeType but an opaque path with no real
extension, so those are matched by mimeType. Plain `file://` sources (older file managers, some
browsers) often set a generic/absent mimeType but a real path, so those are matched by
`pathPattern` instead — repeated with 0/1/2 embedded extra dots per extension, since Android's
`pathPattern` glob is non-greedy and can otherwise fail to match a filename with any dot besides
the extension's own (a known Android limitation; the exact-match `pathSuffix` alternative needs
API 31+, above this app's minSdk 26).

**Dialog, not Box, for the preview popup** (`ui/preview/ArchivePreviewScreen.kt`): the folder-
drill-down card is wrapped in a Compose `Dialog` (with `dismissOnBackPress = false` and its own
`BackHandler` that navigates up a folder before actually dismissing) rather than a plain
full-screen `Box`. That's what lets the same composable work correctly in both places it's used —
as the entire content of the translucent `ArchivePreviewActivity` window, and as an in-app overlay
drawn on top of `MainActivity`'s Home screen — since a `Dialog` brings its own scrim/window
regardless of which activity hosts it, where a bare `Box` would only get one for free in the
dedicated translucent-themed Activity.

**Icons are mostly vendored, not `material-icons-extended`.** `androidx.compose.material:material-
icons-core` turned out to be missing most of what this app needed — `Archive`, `Unarchive`,
`Folder`, `Description`, `CreateNewFolder`, `NoteAdd`, `Visibility`/`VisibilityOff`, `Error` all
failed to resolve (same pattern linker ran into with `Bookmark`/`DragHandle`/`Public`/AutoMirrored
`OpenInNew`). Rather than pull in `material-icons-extended` (a large pack of ~1000 icons this app
would use five of), the archive/folder/file/error glyphs are plain XML vector drawables under
`res/drawable/ic_*.xml`, and the password field's show/hide toggle is a plain "Show"/"Hide"
`TextButton` instead of an icon. `Close`, `CheckCircle`, `Lock`, and AutoMirrored `ArrowBack` did
resolve from core and are used directly.

**Create progress is per top-level pick, not per file inside it** for ZIP (`ZipWriter`) — zip4j's
`addFolder`/`addFile` are single blocking calls with no per-file callback short of a separate
thread + `ProgressMonitor` API, not worth the complexity for a short-lived operation. 7z and
TAR.GZ writers do report true per-file progress since Commons Compress's entry-at-a-time API makes
that free.

## Theme

Same Nord `MaterialTheme` color-role setup as noter/linker's `Theme.kt` (every M3 role filled in
explicitly, including `surfaceContainer*` tiers) — see linker's `CLAUDE.md` for the reasoning
behind why every role needs an explicit value rather than relying on `darkColorScheme()`'s
defaults. `ArchivePreviewActivity` uses `Theme.Zipper.Transparent` the same way linker's
`LinkInterceptorActivity` uses `Theme.Linker.Transparent`.

## Fonts

Martian Mono Nerd Font ships as bundled `.ttf`s under `res/font/` (license in
`app/licenses/MARTIAN_MONO_LICENSE.txt`), applied via `ui/theme/Type.kt` exactly like
ownscreen/noter/linker.
