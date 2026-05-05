# Changelog

All notable changes to PvPIndex MCBans are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Release tags use the `v` prefix (e.g. `v1.0.0`).

---

## [Unreleased]

### Added
### Changed
### Fixed
### Removed

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
