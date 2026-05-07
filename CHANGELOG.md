# Changelog

All notable changes to PvPIndex MCBans are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Release tags use the `v` prefix (e.g. `v1.0.0`).

---

## [Unreleased]

---

## [1.1.0] - 2026-05-07

### Added
- **Customizable kick messages** - `kick-message` section in `config.yml` with per-type templates (`global`, `local`, `temp`, `failsafe`). Supports `{reason}`, `{admin}`, `{expires}`, and `{appeal_url}` placeholders plus an optional `appeal-url` appended automatically.
- **Reason presets** - `reason-presets` section in `config.yml`. Use `#key` in any ban command to expand to a full reason string (`/ban <player> #hacks`). Extended form supports a `default-duration` for use with `/tban <player> #preset`. Tab-completion suggests preset keys.
- **Preset startup validation** - invalid or misconfigured presets (missing reason field, bad duration format) are logged as warnings on load/reload.
- **Case-insensitive preset keys** - `#Hacks`, `#HACKS`, and `#hacks` all resolve to the same preset.
- **Improved tab-completion for ban commands** - preset `#key` suggestions appear at the reason position even on empty input (no need to type `#` first); sorted alphabetically; filtered case-insensitively.
- **`/mcbans presets` command** - lists all configured presets with their reason text and default duration at a glance. Requires `mcbans.ban.local`.
- Config bumped to `ConfigVersion: 3`; upgrading servers get their old config backed up automatically.

---

## [1.0.0] - 2026-05-04

### Added
- Multi-module Maven build: `pvpindex-mcbans-api` (JitPack-publishable HTTP client) and `pvpindex-mcbans-plugin` (Paper plugin fat JAR).
- `jitpack.yml` - JitPack CI support for `pvpindex-mcbans-api`.
- MySQL / MariaDB and PostgreSQL storage backends via HikariCP.
- `rban` command for banning offline (never-joined) players.
- PlaceholderAPI expansion.

### Changed
- Project rewritten from scratch under the PvPIndex platform; original MCBans API replaced with PvPIndex REST API.
- Storage layer refactored: SQLite remains the default; MySQL and PostgreSQL added as optional backends.
- Permission backend now configurable: `SuperPerms` (default), `Vault`, or `OPs`.
- Background sync uses a delta endpoint (`/bans/sync`) instead of full re-download.
- Failsafe mode is now opt-in (`failsafe: false` default) rather than enabled by default.

### Fixed
- String reference-equality comparisons (`==`) in `MCBansAPI` replaced with `isEmpty()`.
- Duplicate `@param` in `MCBansAPI.tempBan` javadoc.

---

[Unreleased]: https://github.com/PVP-Index/pvpindex-mcbans/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/PVP-Index/pvpindex-mcbans/releases/tag/v1.0.0
