# Plan: Disk Space Investigation App

## Meta

- **Date:** 2026-03-27
- **Platform:** Desktop (Windows, macOS, Linux)
- **Build:** Gradle Kotlin DSL + Compose Multiplatform plugin
- **JDK:** 21

## Overview

Build a Kotlin Compose Desktop app that scans local or SSH-remote directory trees, computes file/directory sizes with aggregation, and presents a navigable size-sorted file browser. Users can delete files to reclaim space, save/load sessions as compact CBOR+gzip files, and open files in the OS file browser.

## File Manifest

### Project root config
- `build.gradle.kts` — root build with Compose plugin, all dependencies
- `settings.gradle.kts` — project name
- `gradle.properties` — JVM args, Compose config
- `gradle/wrapper/*` — Gradle wrapper (8.x)
- `src/main/proto/` — (not needed, using CBOR)

### Source: `src/main/kotlin/com/diskspaceinvestigator/`
- `Main.kt` — application entry point, window setup
- **model/**
  - `FileNode.kt` — core data class: name, size, isDirectory, children, path
  - `ScanSource.kt` — sealed class: LocalSource(path) | SshSource(host, user, path, port)
  - `ScanProgress.kt` — data class: filesScanned, bytesTotal, currentPath, phase
  - `SortMode.kt` — enum: SIZE_DESC, NAME_ASC
  - `Session.kt` — data class: source, rootNode, timestamp, metadata
- **scanner/**
  - `Scanner.kt` — interface with `suspend fun scan(source: ScanSource): Flow<ScanProgress>`
  - `LocalScanner.kt` — java.nio.file walkFileTree implementation
  - `SshScanner.kt` — Apache MINA SSHD SFTP-based scanning
  - `ScanResult.kt` — data class wrapping completed FileNode tree + stats
- **ssh/**
  - `SshConnectionManager.kt` — connection setup, key discovery, auth flow
  - `SshConfig.kt` — parse ~/.ssh/config for host aliases
- **session/**
  - `SessionManager.kt` — save/load sessions, list previous sessions, estimate size
  - `SessionStore.kt` — CBOR+gzip serialization/deserialization
- **delete/**
  - `FileDeleter.kt` — interface for delete operations
  - `LocalDeleter.kt` — recursive Files.delete
  - `SshDeleter.kt` — SFTP-based recursive delete
- **viewmodel/**
  - `AppViewModel.kt` — main state holder: screen state, scan state, navigation state
  - `AppState.kt` — sealed class for screen states (Start, Scanning, Browsing)
- **ui/**
  - `App.kt` — root composable, screen routing
  - `theme/Theme.kt` — Fluent UI theme setup, dark mode config
  - `screens/StartScreen.kt` — source picker (local file chooser + SSH input), session list
  - `screens/ScanningScreen.kt` — progress display (files scanned, bytes, current path)
  - `screens/BrowsingScreen.kt` — file navigator with size, sort toggle, delete, open-in-browser
  - `components/FileRow.kt` — single row: icon, name, size, action buttons
  - `components/SshConnectDialog.kt` — host/user/path input + passphrase dialog
  - `components/DeleteConfirmDialog.kt` — confirmation with file name, size, count
  - `components/SessionListItem.kt` — previous session: source, date, size, load button
  - `components/SizeDisplay.kt` — human-readable size formatting composable

### Tests: `src/test/kotlin/com/diskspaceinvestigator/`
- **model/**
  - `FileNodeTest.kt` — size aggregation, tree building, sorting
- **scanner/**
  - `LocalScannerTest.kt` — temp directory scanning, edge cases (empty, deep, permission denied)
  - `SshScannerTest.kt` — embedded SSHD server test
- **session/**
  - `SessionStoreTest.kt` — CBOR+gzip round-trip, size estimation accuracy
- **delete/**
  - `LocalDeleterTest.kt` — file + directory deletion, tree update
- **ssh/**
  - `SshConnectionManagerTest.kt` — key discovery, auth flow with embedded server
- **viewmodel/**
  - `AppViewModelTest.kt` — state transitions, scan lifecycle
- **ui/**
  - `StartScreenTest.kt` — session list rendering, source selection
  - `BrowsingScreenTest.kt` — navigation, sorting, delete flow

## Implementation Steps

### Phase 1: Project Scaffolding
1. Initialize Gradle wrapper and `build.gradle.kts` with:
   - Kotlin 2.1.x, Compose Multiplatform 1.7.x
   - Compose Fluent UI dependency
   - Apache MINA SSHD (core + sftp)
   - kotlinx.serialization (core + cbor)
   - kotlinx.coroutines
   - JUnit 5 + Compose testing
   - Native distribution config for all 3 OSes
2. Create `settings.gradle.kts` and `gradle.properties`
3. Create package directory structure
4. Verify `gradle build` compiles empty project

### Phase 2: Core Data Model
5. Implement `FileNode` — name, absolutePath, size (Long), isDirectory, children (mutable list), parent reference (weak/nullable)
   - `totalSize` computed property: own size + children's totalSize (cached)
   - Size aggregation: computed bottom-up after scan completes
   - Sorting: `sortedBySize()` and `sortedByName()` returning sorted children
6. Implement `ScanSource`, `ScanProgress`, `SortMode`, `Session` data classes
7. Write `FileNodeTest` — verify aggregation, sorting, edge cases (empty dir, single file)

### Phase 3: Local Scanner
8. Implement `LocalScanner` using `Files.walkFileTree()`
   - `FileVisitor` that builds `FileNode` tree as it walks
   - Emits `ScanProgress` via `MutableStateFlow` every 100 files or 500ms
   - Handles: symlinks (skip, record as 0-byte), permission denied (skip, count), empty dirs
   - After walk completes, compute aggregate sizes bottom-up
9. Write `LocalScannerTest` — create temp dir trees with known sizes, verify accuracy

### Phase 4: SSH Support
10. Implement `SshConfig` — parse `~/.ssh/config` for Host entries (hostname, user, port, identity file)
11. Implement `SshConnectionManager`:
    - Discover keys in `~/.ssh/` (id_ed25519, id_rsa, id_ecdsa, id_dsa)
    - Try key-based auth, prompt for passphrase if needed (via callback)
    - Fall back to password auth (via callback)
    - Return connected `SftpClient`
12. Implement `SshScanner` — SFTP-based directory walking
    - Recursive `readDir` + `stat` calls
    - Same progress reporting as LocalScanner
    - Multiple SFTP channels for parallelism (configurable, default 4)
13. Write SSH tests with embedded Apache MINA SSHD test server

### Phase 5: Session Management
14. Implement `SessionStore`:
    - `@Serializable` annotations on `FileNode`, `Session`, `ScanSource`
    - `save(session, file)` — CBOR encode → gzip compress → write to file
    - `load(file): Session` — read → gunzip → CBOR decode
    - `estimateSize(session): Long` — serialize to byte array, return length (for UI display)
    - File extension: `.dsi` (disk space investigation)
15. Implement `SessionManager`:
    - Sessions directory: `~/.disk-space-investigator/sessions/`
    - `listSessions()` — list .dsi files with metadata (source, date, file size on disk)
    - `saveSession(session)` — auto-name as `<source>_<timestamp>.dsi`
    - `loadSession(file): Session`
    - `deleteSession(file)` — remove .dsi file
16. Write `SessionStoreTest` — round-trip correctness, gzip compression ratio check

### Phase 6: Delete Operations
17. Implement `LocalDeleter`:
    - `delete(path)` — `Files.delete` for files, recursive for directories
    - Returns total bytes freed
18. Implement `SshDeleter`:
    - SFTP-based recursive delete (list dir → delete files → rmdir)
    - Returns total bytes freed
19. Write `LocalDeleterTest` — verify deletion and byte counting on temp files

### Phase 7: ViewModel
20. Implement `AppState` sealed class:
    - `Start` — initial screen, holds list of previous sessions
    - `Scanning(progress: ScanProgress, source: ScanSource)` — during scan
    - `Browsing(rootNode: FileNode, currentPath: List<FileNode>, source: ScanSource, sortMode: SortMode)` — navigating results
21. Implement `AppViewModel`:
    - `stateFlow: StateFlow<AppState>` — drives UI
    - `startScan(source: ScanSource)` — launches scanner coroutine, transitions Start→Scanning→Browsing
    - `navigateInto(node: FileNode)` — push to currentPath stack
    - `navigateUp()` — pop from currentPath
    - `toggleSort()` — flip between SIZE_DESC and NAME_ASC
    - `deleteNode(node: FileNode)` — confirmation → delete → update tree → refresh state
    - `saveSession()` — serialize current tree
    - `loadSession(file)` — deserialize → transition to Browsing
    - `restart()` — transition back to Start
    - `openInFileBrowser(node: FileNode)` — `Desktop.open()` / `Desktop.browseFileDirectory()`
    - Passphrase/password callbacks: expose `StateFlow<AuthPrompt?>` for UI to show dialog
22. Write `AppViewModelTest` — state transitions, scan lifecycle mock

### Phase 8: UI — Theme & Shell
23. Set up Fluent UI theme in `Theme.kt` — dark mode, accent colors
24. Implement `App.kt` — observe `AppViewModel.stateFlow`, route to correct screen
25. Implement `Main.kt` — window setup, title, size, icon, DI (manual, no framework needed)

### Phase 9: UI — Start Screen
26. Implement `StartScreen.kt`:
    - "Scan Local Directory" button → native file chooser dialog (`JFileChooser` or AWT `FileDialog`)
    - "Scan Remote Directory (SSH)" button → opens `SshConnectDialog`
    - Previous sessions list (from SessionManager) with: source path, scan date, file size of session, "Load" button
    - "Delete Session" option per session
27. Implement `SshConnectDialog.kt`:
    - Fields: host, username (default from system), port (default 22), remote path
    - Dropdown of hosts from `~/.ssh/config`
    - Connect button → triggers auth flow
    - Passphrase dialog (shown if key is encrypted)
    - Password dialog (shown if key auth fails)

### Phase 10: UI — Scanning Screen
28. Implement `ScanningScreen.kt`:
    - Animated progress indicator (Fluent ProgressRing or ProgressBar)
    - Stats: files scanned count, total bytes so far, current directory being scanned
    - Cancel button → cancels coroutine, returns to Start
    - Elapsed time display

### Phase 11: UI — Browsing Screen
29. Implement `BrowsingScreen.kt`:
    - Breadcrumb bar showing current path (clickable segments)
    - Column headers: Name, Size, Actions — sortable by clicking
    - Sort toggle button: "Sort by Size" / "Sort by Name"
    - List of `FileRow` for current directory's children
    - "Save Session" button with estimated size display
    - "Restart" button to go back to Start
30. Implement `FileRow.kt`:
    - Icon (folder vs file, different by type if desired)
    - Name (clickable for directories → navigateInto)
    - Size (human-readable: B, KB, MB, GB, TB, PB)
    - "Open" button → open in OS file browser
    - "Delete" button → triggers DeleteConfirmDialog
31. Implement `DeleteConfirmDialog.kt`:
    - Shows: file/folder name, total size, file count (for dirs)
    - Warning text: "This will permanently delete..."
    - Confirm / Cancel buttons
32. Implement `SizeDisplay.kt` — utility composable for human-readable size formatting
33. Write UI smoke tests for StartScreen and BrowsingScreen

### Phase 12: Integration & Polish
34. End-to-end test: scan a temp directory → browse → verify sizes → save session → reload → verify
35. Handle edge cases: empty root, scan interrupted, SSH disconnect during scan
36. Error handling: show error dialogs for connection failures, permission issues, disk full on save
37. Verify native packaging works on current platform (`gradle createDistributable`)

## Test Coverage

| Area | Test Type | Key Scenarios |
|------|-----------|--------------|
| FileNode | Unit | Aggregation, sorting, empty dirs, deep nesting |
| LocalScanner | Integration | Known temp trees, symlinks, permission denied, empty |
| SshScanner | Integration | Embedded SSHD, auth flows, recursive scan |
| SessionStore | Unit | CBOR+gzip round-trip, large trees, version compat |
| LocalDeleter | Integration | File delete, dir recursive delete, byte count |
| AppViewModel | Unit | State transitions, scan→browse, sort toggle, delete flow |
| UI Screens | UI/Smoke | Rendering, navigation, sort toggle, dialog display |

## Risks

1. **Compose Fluent UI breakage** — Fallback: swap to Material 3 dark theme. Components are wrapped in our own composables, so change is localized to `theme/` and component files.
2. **SSH scan performance** — SFTP stat calls are slow. Mitigation: batch with multiple channels, show progress so user knows it's working.
3. **Large scan memory** — 10M+ files could pressure heap. Mitigation: start with `-Xmx4g`, monitor in testing. SQLite spillover is a future enhancement.
4. **Platform-specific file browser** — `Desktop.open()` behavior varies. Test on each OS.

## Completion Criteria

- [ ] App builds and runs on Windows (dev machine)
- [ ] Local directory scan works with accurate size aggregation
- [ ] SSH remote scan works with key + passphrase auth
- [ ] File navigator shows sorted entries with correct sizes
- [ ] Sort toggle (size/name) works
- [ ] Delete files/folders with confirmation
- [ ] Open file/folder in OS file browser
- [ ] Save session to .dsi file, shows estimated size
- [ ] Load previous session from Start screen
- [ ] Restart returns to Start screen
- [ ] Progress indicator during scan with file count + bytes + current path
- [ ] Native distributable packages configured for Windows, macOS, Linux
- [ ] Tests pass with reasonable coverage on core logic
