# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
./gradlew build                  # Compile + test + assemble
./gradlew run                    # Run the desktop app
./gradlew compileKotlin          # Compile only (no tests)
./gradlew test                   # Run all tests
./gradlew test --tests "page.matthewt.diskspaceinvestigator.model.FileNodeTest"           # Single test class
./gradlew test --tests "page.matthewt.diskspaceinvestigator.model.FileNodeTest.testName"  # Single test method
./gradlew packageDistributionForCurrentOS  # Native installer (MSI/DMG/DEB)
```

Tests use JUnit 5 (Jupiter) with `@TempDir` for filesystem tests. Test JVM gets `-Xmx2g`.

## Architecture

State-driven MVVM app: Kotlin Compose Desktop with Fluent UI dark theme.

**State flow:** `AppViewModel` holds a `MutableStateFlow<AppState>` where `AppState` is a sealed class with four variants: `Start` → `Scanning` → `Browsing` (or `Start` → `LoadingSession` → `Browsing` for saved sessions). `App.kt` routes to the corresponding screen based on the current state variant.

**FileNode tree:** The core data structure is a serializable tree (`model/FileNode.kt`) with mutable children and cached aggregates (totalSize, fileCount, directoryCount). After scan or deserialization, call `computeAggregates()` to precompute bottom-up sums (O(n) once, O(1) lookups). On node removal, `invalidateCaches()` propagates up the parent chain. Parent references are `@Transient` and restored via `relinkParents()` after deserialization.

**Scanner interface:** `scanner/Scanner.kt` defines `scan(): FileNode` + `progress: StateFlow<ScanProgress>` + `cancel()`. Two implementations: `LocalScanner` (uses `Files.walkFileTree`) and `SshScanner` (Apache MINA SSHD SFTP). Both build the FileNode tree during traversal and emit progress updates.

**Session persistence:** Sessions serialize via kotlinx.serialization CBOR + gzip to `.dsi` files in `~/.disk-space-investigator/sessions/`. A `.meta` sidecar stores display name (line 1), total bytes (line 2), and scan duration (line 3) so the session list loads without deserializing full trees.

**SSH auth flow:** `SshConnectionManager` discovers keys from `~/.ssh/`, parses `~/.ssh/config`. When a passphrase or password is needed, it suspends via `CompletableDeferred` while the ViewModel shows an `AuthPrompt` dialog. The user's response completes the deferred to resume auth.

**Deletion:** `FileDeleter` interface with `LocalDeleter` (recursive `Files.walkFileTree` + delete) and `SshDeleter` (SFTP-based, partially implemented). Always permanent delete — no trash.

## Key Dependencies

- **UI:** Compose Desktop 1.7.3 + Fluent UI (`com.konyaco:fluent:0.0.1-dev.8`). Material3 `OutlinedTextField` used for text inputs (Fluent's TextField API is incompatible with String values).
- **Serialization:** kotlinx-serialization-cbor 1.7.3
- **SSH:** Apache MINA SSHD 2.14.0 + BouncyCastle 1.79
- **JDK:** 21 (via `jvmToolchain`)

## Package Namespace

`page.matthewt.diskspaceinvestigator` — derived from the `matthewt.page` domain.
