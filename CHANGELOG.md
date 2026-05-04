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

## [1.0.0] — 2026-05-04

### Added
- Multi-module Maven build: `pvpindex-mcbans-api` (JitPack-publishable HTTP client) and `pvpindex-mcbans-plugin` (Paper plugin fat JAR).
- `jitpack.yml` — JitPack CI support for `pvpindex-mcbans-api`.
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

## [5.1.x] — legacy PvPIndex port

Internal iteration series. Migrated original MCBans 4.x codebase to Paper 1.21 and the PvPIndex API.

---

## [4.21] — legacy

### Fixed
- RCON permission issue.

## [4.2] — legacy

### Added
- Bukkit default ban/ipban list integration for some commands.
- Cancellable `IPBanEvent` and `IPBannedEvent`.
- Updated Swedish locale (`sv-se`) — thanks oggehej.

### Fixed
- Error during ban syncing.

## [4.1] — legacy

### Added
- Detailed previous-ban message on join.
- `sendDetailPrevBansOnJoin` configuration option.
- `mcbans.kick.exempt` permission node.
- Auto-regeneration of missing locale files on `/mcbans reload`.

### Fixed
- Language file warning.
- Remaining-time format.
- Ban sync.

## [4.0] — legacy

### Added
- `MCBansAPI` class for third-party plugin integration.
- Vault permission support.
- API key validity check.
- Player name validity check.
- Config file version check (auto-recreate on mismatch).
- Additional localisable messages.
- Timeout configuration.
- Failsafe configuration.
- Player lookup API.
- `/banlookup` command and API.
- Colored message support (`&<char>`).
- `mcbans.hideview` permission.
- `/altlookup` command (premium servers).
- `/banip` command; unban with `/unban <IP>`.

### Changed
- Rewrote permission structure and nodes.
- Rewrote command structure.
- Rewrote localisation structure.
- Rewrote logging structure.
- Renamed `settings.yml` to `config.yml`; rewrote configuration structure.

### Fixed
- `enableMaxAlts` configuration not working.
- Minor exception on player join.
- Incorrect sync interval.
- `syncBans` configuration not working (moved to `enableAutoSync`).
- Temp ban `w` (week) measure not working.
- `onJoinMCBansMessage` configuration not working.
- Kicking the wrong player.

## [3.9 R1] — legacy

### Added
- Rollback integrations: CoreProtect, Hawkeye, LogBlock.
- `/mcbans sync all` to force a full resync.
- Throttling for sync/callback.
- Bukkit events for plugin actions.

### Removed
- Second API call per login (performance improvement).
- NoCheatPlus data reporting.
- LogBlock data reporting.

### Fixed
- Sync and callback thread timing.

---

[Unreleased]: https://github.com/PVP-Index/pvpindex-mcbans/compare/v5.2.0...HEAD
[5.2.0]: https://github.com/PVP-Index/pvpindex-mcbans/releases/tag/v5.2.0
