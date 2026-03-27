# Release

Create a new release with the given version number.

## Usage

```
/release <version>
```

Example: `/release 1.0.0` creates tag `v1.0.0` and triggers the release workflow.

## Instructions

You are performing a release of this project. The version argument is: $ARGUMENTS

The version in `gradle.properties` (`appVersion`) is the source of truth — no `v` prefix.
Tags and release names use the `v` prefix (e.g. `v1.0.0`).
Any version shown to the user should include the `v` prefix.

Follow these steps:

### 1. Validate

- Confirm the version argument is a valid semver (e.g. `1.0.0`, `0.2.1`). Strip a leading `v` if the user included one. If invalid, ask the user.
- Check that the working tree is clean (`git status`). If there are uncommitted changes, warn the user and ask whether to proceed.
- Check that the tag `v<version>` does not already exist. If it does, stop and tell the user.

### 2. Bump version, commit, and push

- Update `appVersion` in `gradle.properties` to the new version (no `v` prefix).
- Commit this change: `Bump version to v<version>`
- Push the commit to origin: `git push origin main`

### 3. Tag and push

- Create an annotated git tag: `git tag -a v<version> -m "Release v<version>"`
- Push the tag: `git push origin v<version>`
- Tell the user the tag has been pushed and the GitHub Actions release workflow is now running.

### 4. Generate changelog (do this immediately, don't wait for the workflow)

- Find the previous release tag: `git tag --sort=-v:refname | grep '^v' | head -2` — take the second entry. If there is no previous tag, use the root commit.
- Run `git log <previous-tag>..v<version> --pretty=format:"- %s" --no-merges` to get the list of commits since the last release.
- Read through the commits and write a concise, human-friendly changelog grouped by category (e.g. Features, Fixes, Improvements, Internal). Drop trivial commits (merge commits, typo fixes) unless they're the only changes. Write it in markdown.

### 5. Ask about limitations

- Ask the user: "Are there any known limitations or caveats for this release? (press Enter to skip)"
- If they provide limitations, add a **Known Limitations** section to the changelog.

### 6. Present the changelog for approval

- Show the full changelog to the user and ask: "Does this changelog look good? (yes/edit/cancel)"
- If they say edit, ask what to change and regenerate.
- If they say cancel, stop — the tag and release are already pushed, tell them they can delete the release manually on GitHub.
- If they approve, proceed.

### 7. Update the GitHub release

- Check that the release for `v<version>` exists on GitHub: `gh release view v<version>`
- If the release doesn't exist yet, tell the user: "The GitHub Actions workflow hasn't created the release yet. Waiting..." Retry up to 3 times with 15-second intervals before giving up. If it still doesn't exist, tell the user to check the Actions tab and manually update the release notes once it's created.
- Once the release exists, update its body with the changelog: `gh release edit v<version> --notes "<changelog>"`
- Tell the user the release is complete and provide the release URL.
