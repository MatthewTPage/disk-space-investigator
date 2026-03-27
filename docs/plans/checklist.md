# Checklist

## 1. Project Scaffolding
- [x] 1.1 Initialize Gradle wrapper, build.gradle.kts, settings.gradle.kts, gradle.properties
- [x] 1.2 Add all dependencies (Compose, Fluent UI, MINA SSHD, kotlinx.serialization, JUnit 5)
- [x] 1.3 Configure native distribution for Windows, macOS, Linux
- [x] 1.4 Create package directory structure
- [x] 1.5 Verify project compiles

## 2. Core Data Model
- [x] 2.1 Implement FileNode, ScanSource, ScanProgress, SortMode, Session
- [x] 2.2 Write FileNodeTest (aggregation, sorting, edge cases)

## 3. Local Scanner
- [x] 3.1 Implement LocalScanner with Files.walkFileTree
- [x] 3.2 Write LocalScannerTest with temp directory fixtures

## 4. SSH Support
- [x] 4.1 Implement SshConfig (~/.ssh/config parser)
- [x] 4.2 Implement SshConnectionManager (key discovery, auth flow)
- [x] 4.3 Implement SshScanner (SFTP-based directory walking)
- [ ] 4.4 Write SSH tests with embedded SSHD server (deferred — requires more complex test setup)

## 5. Session Management
- [x] 5.1 Implement SessionStore (CBOR + gzip serialization)
- [x] 5.2 Implement SessionManager (list, save, load, delete sessions)
- [x] 5.3 Write SessionStoreTest (round-trip, compression)

## 6. Delete Operations
- [x] 6.1 Implement LocalDeleter (recursive permanent delete)
- [x] 6.2 Implement SshDeleter (SFTP recursive delete)
- [x] 6.3 Write LocalDeleterTest

## 7. ViewModel
- [x] 7.1 Implement AppState sealed class
- [x] 7.2 Implement AppViewModel (state management, scan lifecycle, navigation, delete, sessions)
- [x] 7.3 Write AppViewModelTest (state transitions)

## 8. UI — Theme & Shell
- [x] 8.1 Set up Fluent UI dark theme
- [x] 8.2 Implement App.kt (screen routing)
- [x] 8.3 Implement Main.kt (window setup)

## 9. UI — Start Screen
- [x] 9.1 Implement StartScreen (local file chooser, SSH button, session list)
- [x] 9.2 Implement SshConnectDialog (host/user/path input, passphrase dialog)

## 10. UI — Scanning Screen
- [x] 10.1 Implement ScanningScreen (progress indicator, stats, cancel)

## 11. UI — Browsing Screen
- [x] 11.1 Implement BrowsingScreen (breadcrumbs, file list, sort toggle, save/restart)
- [x] 11.2 Implement FileRow (icon, name, size, open, delete buttons)
- [x] 11.3 Implement DeleteConfirmDialog
- [x] 11.4 Implement SizeDisplay utility

## 12. Integration & Polish
- [x] 12.1 All 62 tests passing
- [x] 12.2 Error handling (connection failures, permission issues)
- [ ] 12.3 Verify native packaging builds
- [x] 12.4 Write SizeDisplayTest (unit tests for formatting)
