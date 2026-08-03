# Changelog

All notable changes to qcmd are documented here. The project follows semantic versioning.

## [1.1.0] - 2026-08-03

### Added

- Built-in `-h` / `--help` actions and optional `-V` / `--version` actions through `@Cmd.version`.
- `ParseAction`, `ParsedCommand.shouldExit()`, and `ParsedCommand.outputText()` for normal help/version control flow.
- Standalone `QCmd.help(...)` APIs.
- Recursive conversion for nested generic collections and map values.
- CI coverage gates for line and branch coverage.

### Fixed

- Value-taking options no longer consume a following option token as their value.
- Unknown options at the end of the command line now produce `UnknownOptionException`.
- Negative decimals and scientific notation remain valid option values.
- Alias precedence is deterministic: the last occurrence on the command line wins.
- JUnit 4 tests and the previously unannotated integration test now run under JUnit 5.

### Changed

- Descriptor and parse-result collections are exposed as read-only snapshots.
- Annotation-declared converters are instantiated per parse request instead of being globally cached.
- GPG signing and Central publishing run only in the Maven `release` profile.
- Java 17 compilation uses `--release 17`.
- Documentation now describes supported POSIX/GNU-style syntax without claiming complete compatibility.

[1.1.0]: https://github.com/uaholic/qcmd/compare/v1.0.1...v1.1.0
