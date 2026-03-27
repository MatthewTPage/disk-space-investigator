# Disk Space Investigator

A cross-platform desktop app for analyzing and managing disk space usage on local and remote directories.

Built with Kotlin, Compose Desktop, and Fluent UI (Windows 11 dark mode aesthetic).

## Features

- **Local scanning** — Pick any directory and scan it to see a full breakdown of disk usage
- **Remote scanning** — Connect to remote servers via SSH/SFTP using your `~/.ssh` credentials
- **Interactive file browser** — Navigate the scanned tree, sorted by size or name
- **Delete files** — Permanently delete files and directories to free up space
- **Open in file browser** — Jump to any file or folder in your OS file manager
- **Save & load sessions** — Save scan results to compact binary files (.dsi) and reload them later

## Requirements

- JDK 21+

## Build & Run

```bash
./gradlew run                                # Run the app
./gradlew build                              # Compile + test
./gradlew packageDistributionForCurrentOS    # Build native installer (MSI/DMG/DEB/RPM)
```

## Testing Status

- **Windows** — Tested and working
- **macOS** — Not yet tested
- **Linux** — Not yet tested
- **Remote SSH scanning** — Not yet tested

## Known Limitations

- Binaries are unsigned. On macOS you may see an "unidentified developer" warning (right-click > Open to bypass). On Windows, SmartScreen may show a warning on first run.
- SSH file deletion is partially implemented
- Fluent UI dependency (`com.konyaco:fluent`) is a dev preview

## License

[MIT](LICENSE)
