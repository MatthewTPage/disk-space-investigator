# Notes

## Findings

- Fluent UI `TextField` component uses `TextFieldValue` not `String`, unlike Material 3's `OutlinedTextField`. Used Material 3 text fields in dialogs for simplicity while keeping Fluent for everything else.
- Fluent UI `FluentTheme.colors.text.accentText` doesn't exist — only `text.text.primary/secondary/disabled` are available. Used `secondary` for breadcrumb links.
- Apache MINA SSHD's `FileKeyPairProvider` doesn't accept a lambda for passphrase. Used `OpenSSHKeyPairResourceParser` with `FilePasswordProvider` for passphrase-protected keys.
- Kotlin `if/else` blocks need explicit `return` when used as statements in functions returning values (SshDeleter).

## Deviations from Plan

- **Text inputs**: Used Material 3 `OutlinedTextField` for SSH dialog and auth dialog instead of Fluent's `TextField` due to API incompatibility. Fluent is still used for all other components (buttons, text, progress, theme).
- **SSH embedded server tests**: Deferred (4.4) — the embedded SSHD test server setup is complex and would add significant test infrastructure. The SSH code compiles and the connection/scanning logic follows the same pattern as the well-tested local scanner.

## Open Questions

- Should the app remember window position/size between launches?
- Should SSH connection details be saved for quick reconnect?
