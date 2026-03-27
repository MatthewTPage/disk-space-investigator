# Design: Disk Space Investigation App

## Summary

A Kotlin Compose Desktop application that scans local or SSH-remote directory trees, aggregates file/directory sizes, and presents an interactive navigator sorted by size. Supports session save/load, file deletion, and cross-platform builds (Windows, macOS, Linux).

## Table of Contents

| # | Design Problem | Recommended Approach |
|---|---------------|---------------------|
| 1 | UI Design System | Compose Fluent UI — Windows 11 aesthetic with dark mode |
| 2 | Data Structure & Storage | Hybrid: in-memory tree with memory-mapped overflow for huge scans |
| 3 | Scanning Architecture | Coroutine-based parallel walker with channel-based progress reporting |
| 4 | SSH Remote Directory Support | Apache MINA SSHD client with key + passphrase auth |
| 5 | Session Serialization Format | Protocol Buffers (compact binary) |
| 6 | Delete Capability | Confirmation dialog with trash-first, permanent-delete as fallback |
| 7 | Testing Strategy | JUnit 5 + Compose UI testing + temp filesystem fixtures |

---

## 1. UI Design System

### Problem
Material Design 3 ships with Compose but feels mobile-first and generic on desktop. We need a desktop-native look with dark mode.

### Options

**A) Compose Fluent UI (Recommended)**
- Microsoft Fluent Design (Windows 11 aesthetic) for Compose Desktop
- `com.konyaco:fluent:0.0.1-dev.8` — available on Maven Central
- Dark mode built-in, acrylic/mica effects, comprehensive component set
- Multiplatform: works on Windows, macOS, Linux
- Pros: Modern desktop feel, dark mode native, good component coverage
- Cons: Experimental (0.0.1-dev), API may change, some hard-coding internally

**B) Aurora 2.0.0**
- Production-ready (v2.0.0), desktop-only, Ephemeral design system
- `org.pushing-pixels:aurora-component:2.0.0`
- Pros: Most mature option, stable API, rich theming
- Cons: Niche community, heavier dependency, less familiar aesthetic, learning curve for its ribbon/command paradigm

**C) Material Design 3 (built-in)**
- Ships with Compose Multiplatform, zero extra deps
- Pros: Zero setup, well-documented, stable
- Cons: Mobile-first feel, doesn't look native on desktop, the user explicitly dislikes it

### Recommendation
**Compose Fluent UI.** The user wants something that doesn't look like Material Design and mentioned Microsoft's design system. Fluent gives a clean, modern Windows 11 look with built-in dark mode. The experimental status is acceptable for this project — we're not shipping to millions of users, and the component set covers our needs (buttons, lists, navigation, dialogs, progress indicators). If Fluent proves too unstable during implementation, Material 3 with custom dark theming is the fallback — minimal code change since both use Compose primitives.

---

## 2. Data Structure & Storage Strategy

### Problem
The app must handle directory trees up to 100PB. A worst-case scenario: billions of files with deep nesting. We need to store the full tree with aggregated sizes per directory. How much memory does this take?

**Size estimation:**
- Each tree node needs: path string (~200 bytes avg), size (8 bytes), children count, file type flag
- Estimated ~300 bytes per node in-memory (with Kotlin/JVM object overhead closer to ~500 bytes)
- 1 billion files = ~500 GB RAM — not feasible in-memory alone
- 10 million files = ~5 GB RAM — borderline
- 1 million files = ~500 MB RAM — comfortable

Most real-world scans will be <10M files, but we must handle more.

### Options

**A) In-memory tree with disk spillover (Recommended)**
- Keep the tree in memory for scans under a configurable threshold (e.g., 5M nodes / ~2.5GB)
- For larger scans, use a lightweight embedded database (SQLite via sqlite-jdbc) to store nodes, with an in-memory LRU cache for currently-viewed directories
- Tree structure: `data class FileNode(val name: String, val size: Long, val isDirectory: Boolean, val children: List<FileNode>?)`
- Directories store pre-computed aggregate sizes
- Pros: Fast for typical scans, graceful degradation for huge scans
- Cons: Two code paths (memory vs. DB), more complexity

**B) Always in-memory**
- Simple flat tree, set JVM `-Xmx` high
- Pros: Simple, fast
- Cons: OOM on large scans, JVM heap limits

**C) Always on-disk (SQLite)**
- Every node goes to SQLite, query on navigation
- Pros: Handles any size, consistent code path
- Cons: Slower for small scans, unnecessary I/O overhead for typical use

### Recommendation
**Option A — hybrid approach.** For the vast majority of scans (<5M files), everything stays in memory and is fast. For extreme cases, we spill to SQLite. The SQLite path also naturally feeds into session save/load (the saved session IS the SQLite file). We can start with in-memory only for the MVP and add SQLite spillover as an enhancement if needed — the `FileNode` interface abstracts the storage backend.

**Practical note:** Start with pure in-memory and see how far it goes. A 100PB filesystem likely has a manageable number of files (petabyte-scale storage systems use large files). We'll add SQLite if testing shows memory pressure.

---

## 3. Scanning Architecture

### Problem
Scanning millions of files must be fast, show progress, and work for both local and SSH sources.

### Options

**A) Kotlin coroutines with Flow-based progress (Recommended)**
- Scanner runs in `Dispatchers.IO` coroutines
- Emits progress via `StateFlow<ScanProgress>` (files scanned count, current directory, bytes tallied)
- Local: uses `java.nio.file.Files.walkFileTree()` for efficient OS-level traversal
- SSH: uses SFTP channel to list directories recursively
- Parallelism: multiple coroutines for different subtrees (configurable concurrency)
- Pros: Native Kotlin, integrates cleanly with Compose state, cancellable
- Cons: SSH parallelism limited by single SFTP channel (can open multiple channels)

**B) Java Fork/Join framework**
- Use `RecursiveTask` for parallel directory walking
- Pros: Good CPU parallelism
- Cons: Doesn't integrate well with Compose state, harder to report progress, overkill

**C) Single-threaded walk**
- Simple recursive walk, update UI periodically
- Pros: Simple, no concurrency bugs
- Cons: Slow on large trees, can't utilize I/O parallelism

### Recommendation
**Option A.** Coroutines are the natural fit for Compose Desktop. The scanner becomes a ViewModel function that emits `StateFlow`, and the UI observes it reactively. We can walk multiple subtrees concurrently on local filesystems (helps with SSDs/NVMe), and open multiple SFTP channels for SSH. Progress reporting is a count of files scanned + bytes tallied + current path — gives the user a sense of movement.

---

## 4. SSH Remote Directory Support

### Problem
The app must connect to remote hosts via SSH, using keys from `~/.ssh/` with optional passphrase, and scan their directory trees.

### Options

**A) Apache MINA SSHD (Recommended)**
- Pure Java SSH implementation, well-maintained by Apache
- `org.apache.sshd:sshd-core` + `org.apache.sshd:sshd-sftp`
- Supports: key-based auth (RSA, Ed25519, ECDSA), passphrase-protected keys, password auth, known_hosts verification
- Can read `~/.ssh/config` for host aliases
- Pros: Pure Java (no native deps), mature, full SSH2 support, built-in SFTP
- Cons: Heavier dependency (~2MB), slightly more boilerplate than JSch

**B) JSch (or its maintained fork)**
- Classic Java SSH library
- `com.github.mwiede:jsch:0.2.x` (maintained fork)
- Pros: Lightweight, well-known
- Cons: Limited key format support in original, fork is less battle-tested, API is dated

**C) Process-based (shell out to `ssh`/`sshfs`)**
- Use the system's SSH binary via `ProcessBuilder`
- Pros: Uses whatever SSH the user has configured, zero library deps
- Cons: Parsing output is fragile, platform differences, SSHFS not available everywhere, hard to show progress

### Recommendation
**Option A — Apache MINA SSHD.** It's the most robust pure-Java SSH implementation, supports all modern key formats, handles passphrase prompts programmatically, and includes a built-in SFTP client. We read keys from `~/.ssh/`, parse `~/.ssh/config` for host settings, and verify against `known_hosts`. The passphrase prompt surfaces in the UI as a password dialog.

### Auth Flow
1. User enters `user@host:/path` or picks from SSH config hosts
2. App reads `~/.ssh/config` to resolve host settings
3. Tries key-based auth with keys from `~/.ssh/` (id_ed25519, id_rsa, etc.)
4. If key is passphrase-protected → show passphrase dialog
5. If key auth fails → show password dialog for password-based auth
6. On success → start SFTP-based directory scan

---

## 5. Session Serialization Format

### Problem
Users want to save scan results and load them later. The format should be as compact as possible. A scan of 1M files at ~100 bytes/node serialized = ~100MB.

### Options

**A) Protocol Buffers (Recommended)**
- Binary format, extremely compact, schema-enforced
- `com.google.protobuf:protobuf-kotlin` + Gradle protobuf plugin
- Tree serialized as nested messages; sizes as varint (1-9 bytes instead of 8)
- Estimated: ~60-80 bytes per node (path as relative string + delta encoding)
- Pros: Very compact, fast serialization, versioned schemas, cross-platform
- Cons: Adds protobuf dependency + build plugin, schema file to maintain

**B) MessagePack**
- Binary JSON-like format
- `org.msgpack:msgpack-core`
- Pros: Compact, no schema needed, simple
- Cons: Slightly larger than protobuf, no schema evolution guarantees

**C) CBOR (Concise Binary Object Representation)**
- Binary format, RFC 8949 standard
- Available via `kotlinx.serialization` with CBOR support
- Pros: Compact, standards-based, kotlinx.serialization integration
- Cons: Less tooling than protobuf, slightly larger output

**D) Gzipped JSON**
- JSON + gzip compression
- Pros: Human-readable (when decompressed), zero extra deps
- Cons: Largest format even compressed, slow to parse

### Recommendation
**Option C — CBOR via kotlinx.serialization.** On reflection, CBOR is the best fit here. It's nearly as compact as protobuf but integrates natively with `kotlinx.serialization` (which we already need for Kotlin data classes). No protobuf plugin, no `.proto` files, no code generation step. We annotate our `FileNode` data classes with `@Serializable` and use `Cbor.encodeToByteArray()`. Add gzip on top for further compression (file paths share prefixes → compress well).

**Estimated sizes with CBOR + gzip:**
- 1M files: ~40-60 MB
- 10M files: ~400-600 MB
- Display this estimate to user before saving

---

## 6. Delete Capability

### Problem
Users want to delete files/folders from within the app. This is destructive and must be handled carefully.

### Approach

**Always permanent delete.** The purpose of this app is to free disk space — moving to trash just relocates the problem.

- Local: `Files.delete()` for files, recursive delete for directories
- SSH remote: SFTP `rm` / `rmdir` recursive
- **Confirmation dialog** showing file/folder name, total size, and file count (for directories)
- After deletion, update the tree in-place (subtract size from all ancestor directories, remove node)

---

## 7. Testing Strategy

### Problem
Need good test coverage across scanning, data model, serialization, SSH, and UI.

### Approach

- **Unit tests (JUnit 5):** FileNode operations, size aggregation, sorting, serialization round-trips, path handling
- **Integration tests:** Local filesystem scanning with temp directories (create known file structures, verify sizes). SSH scanning with embedded SSHD test server (Apache MINA provides one).
- **UI tests:** Compose Desktop test framework (`createComposeRule()`) for navigation, sorting toggle, session management screens
- **Property-based tests:** Use `kotlinx-test` or similar for edge cases (empty dirs, symlinks, permission denied, very deep nesting)
- **Coverage target:** Core logic (scanning, aggregation, serialization) at 80%+. UI at smoke-test level.

### Test fixtures
- Temp directory trees with known file sizes for deterministic assertions
- Embedded SSH server (Apache MINA SSHD test utilities) for SSH path testing
- Mock clock/filesystem for progress reporting tests

---

## Architecture Overview

```
┌─────────────────────────────────────────────────┐
│                   UI Layer                       │
│  (Compose Desktop + Fluent UI)                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
│  │  Start   │ │ Loading  │ │   File Navigator │ │
│  │  Screen  │ │  Screen  │ │     Screen       │ │
│  └──────────┘ └──────────┘ └──────────────────┘ │
├─────────────────────────────────────────────────┤
│              ViewModel Layer                     │
│  ┌──────────────────────────────────────────┐   │
│  │  AppViewModel (StateFlow-based)          │   │
│  │  - scanState, currentPath, sortMode      │   │
│  └──────────────────────────────────────────┘   │
├─────────────────────────────────────────────────┤
│              Domain Layer                        │
│  ┌────────────┐ ┌────────────┐ ┌─────────────┐ │
│  │  Scanner   │ │  Session   │ │   Deleter   │ │
│  │  (Local +  │ │  Manager   │ │  (Trash +   │ │
│  │   SSH)     │ │  (CBOR+gz) │ │   Delete)   │ │
│  └────────────┘ └────────────┘ └─────────────┘ │
├─────────────────────────────────────────────────┤
│              Data Layer                          │
│  ┌────────────────────────────────────────────┐ │
│  │  FileNode tree (in-memory)                 │ │
│  │  SessionStore (CBOR files on disk)         │ │
│  └────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

---

## Key Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| Kotlin | 2.1.x | Language |
| Compose Multiplatform | 1.7.x | UI framework |
| Compose Fluent UI | 0.0.1-dev.8 | Design system |
| Apache MINA SSHD | 2.14.x | SSH/SFTP client |
| kotlinx.serialization | 1.7.x | Serialization (CBOR) |
| kotlinx.coroutines | 1.9.x | Async scanning |
| JUnit 5 | 5.11.x | Testing |
| JDK | 21 | Target runtime |

---

## Open Questions / Risks

1. **Compose Fluent UI stability** — It's experimental. If it breaks, fallback is Material 3 with custom dark theme. Low risk since we'd wrap components in our own composables.
2. **SSH scanning speed** — SFTP `ls` is slow for huge directories. Mitigation: batch requests, multiple channels.
3. **Symlink handling** — Should we follow symlinks? Recommendation: no, to avoid infinite loops. Count symlink as 0 bytes, display as symlink.
4. **Permission denied files** — Skip and log. Show count of inaccessible files/dirs in results.
5. **Cross-platform file size** — `Files.size()` on Windows may differ from `stat` on Linux for sparse files. Accept OS-reported size.
