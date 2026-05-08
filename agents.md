# PvPIndex MCBans - Agent Guide

## Overview

**PvPIndex MCBans** is a Paper 1.21 plugin that enforces player bans through the PvPIndex REST API (`https://api.pvpindex.com`). It maintains a local SQLite cache so bans are enforced even when the API is unreachable.

- **Package:** `com.pvpindex.bans`
- **Java version:** 21
- **Build:** Maven (`mvn clean package`)
- **Test:** `mvn test` (JUnit 5 + MockBukkit)
- **Style:** `mvn checkstyle:check`

---

## Repository Structure

```
pvpindex-mcbans/
├── pom.xml                              # Maven build (Java 21, Paper 1.21, sqlite-jdbc, JUnit 5)
├── checkstyle.xml                       # Code style rules (4-space, 120-char, no wildcards)
├── checkstyle-suppressions.xml          # Suppressions for legacy code
├── .github/
│   └── workflows/ci.yml                 # CI: checkstyle job + test+codecov job
└── src/
    ├── main/
    │   ├── java/com/pvpindex/bans/
    │   │   ├── api/                     # REST client + DTOs
    │   │   │   ├── PvPIndexApiClient    # java.net.http.HttpClient, 3s connect / 5s req
    │   │   │   ├── BanRecord            # API ban DTO (record)
    │   │   │   ├── BanRequest           # API ban submission DTO (record)
    │   │   │   ├── BanStatusResponse    # { banned, ban? } (record)
    │   │   │   └── BanSyncPage          # Paginated delta sync response (record)
    │   │   ├── storage/                 # SQLite offline cache
    │   │   │   ├── StorageManager       # Opens bans.db, creates schema
    │   │   │   ├── BanDao               # CRUD for player_bans + meta tables
    │   │   │   └── LocalBan             # Immutable SQLite row (record)
    │   │   ├── plugin/
    │   │   │   ├── MCBans               # Plugin main class (JavaPlugin)
    │   │   │   ├── ConfigurationManager # Reads config.yml (pvpindex.apiUrl/apiKey/syncInterval)
    │   │   │   ├── I18n                 # i18n message lookup
    │   │   │   ├── ActionLog            # Logger wrapper
    │   │   │   ├── BanType              # Enum: GLOBAL / LOCAL / TEMP
    │   │   │   ├── callBacks/BanSync    # Background thread: upload unsynced + download delta
    │   │   │   ├── bukkitListeners/
    │   │   │   │   └── PlayerListener   # AsyncPlayerPreLoginEvent → API check → SQLite fallback
    │   │   │   ├── request/Ban          # Executes ban/unban (writes SQLite + calls API)
    │   │   │   ├── request/Kick         # Kick-only action
    │   │   │   ├── commands/            # /ban /gban /tban /unban /kick /banip /lookup /mcbans etc.
    │   │   │   ├── events/              # Custom Bukkit events (PlayerBanEvent etc.)
    │   │   │   ├── exception/           # CommandException, MCBansException
    │   │   │   ├── permission/          # Perms enum + PermissionHandler
    │   │   │   ├── api/MCBansAPI        # Public API for third-party plugins
    │   │   │   └── util/                # Util, FileStructure, VaultStuff
    │   │   └── utils/                   # IPTools, TimeTools
    │   └── resources/
    │       ├── config.yml               # Default configuration
    │       ├── plugin.yml               # main: com.pvpindex.bans.plugin.MCBans, api-version: 1.21
    │       └── languages/               # i18n message files
    └── test/
        └── java/com/pvpindex/bans/
            ├── storage/BanDaoTest       # In-memory SQLite, no Bukkit required
            └── plugin/MCBansPluginTest  # MockBukkit smoke tests (enable/disable lifecycle)
```

---

## Data Flow

### Login check

```
AsyncPlayerPreLoginEvent
  └─ PlayerListener
       ├─ apiClient.getBanStatus(uuid)  [3 s timeout, async]
       │    ├─ banned=true  → upsertBan in SQLite, disallow login
       │    └─ banned=false → allow login
       └─ API timeout/error
            └─ dao.findActiveBan(uuid) [SQLite fallback]
                 ├─ found  → disallow login (offline enforcement)
                 └─ not found → allow login
```

### Ban command

```
/ban <player> [reason]
  └─ Ban.run()
       ├─ dao.insertOfflineBan(...)    [always, immediate]
       └─ apiClient.ban(BanRequest)   [best-effort, fires on new thread]
            ├─ success → dao.markSynced(uuid)
            └─ failure → left is_synced=0 for BanSync to retry
```

### Background sync

```
BanSync (daemon thread, every syncInterval minutes)
  ├─ uploadUnsynced()
  │    └─ for each is_synced=0 row:
  │         apiClient.ban() or apiClient.unban()
  │         → on success: dao.markSynced()
  └─ downloadDelta()
       └─ GET /plugin/bans?updated_since={lastSyncAt}&page={n}
            → dao.upsertBan() for each record
            → dao.setMeta("lastSyncAt", now)
```

---

## API Endpoints Used

Base URL: `https://api.pvpindex.com` (configurable via `pvpindex.apiUrl`)
Auth: `Authorization: Bearer {pvpindex.apiKey}`

| Method | Path | Purpose |
|--------|------|---------|
| `GET`  | `/plugin/players/{uuid}/ban-status` | Check if player is banned |
| `POST` | `/plugin/players/{uuid}/ban`         | Issue a ban |
| `DELETE` | `/plugin/players/{uuid}/ban`       | Unban |
| `GET`  | `/plugin/bans?updated_since=&page=`  | Delta sync |

---

## SQLite Schema

Database: `plugins/MCBans/bans.db`

```sql
CREATE TABLE player_bans (
  uuid        TEXT PRIMARY KEY,
  player_name TEXT,
  type        TEXT NOT NULL,      -- global | local | temp
  reason      TEXT NOT NULL,
  admin_uuid  TEXT,
  admin_name  TEXT,
  expires_at  INTEGER,            -- Unix epoch seconds, NULL = permanent
  is_active   INTEGER DEFAULT 1,
  is_synced   INTEGER DEFAULT 0,  -- 0 = needs push to API
  created_at  INTEGER NOT NULL,
  updated_at  INTEGER NOT NULL
);

CREATE TABLE meta (
  key   TEXT PRIMARY KEY,
  value TEXT                      -- e.g. lastSyncAt = ISO-8601 timestamp
);
```

---

## Build Commands

```bash
mvn clean package          # Build shaded JAR → target/pvpindex-mcbans-*.jar
mvn checkstyle:check       # Checkstyle only (fails on violation)
mvn test                   # Run JUnit 5 tests (BanDaoTest + MCBansPluginTest)
mvn verify                 # checkstyle + compile + test + JaCoCo coverage report
```

Coverage report: `target/site/jacoco/index.html`

---

## CI Workflow (`.github/workflows/ci.yml`)

Two jobs on every push/PR to `main`, `feature/**`, `fix/**`:

1. **checkstyle** - `mvn checkstyle:check` (fails fast)
2. **test** - `mvn verify -Dcheckstyle.skip=true` → uploads `jacoco.xml` to Codecov

Set the `CODECOV_TOKEN` repository secret to enable Codecov reporting.

---

## Coding Conventions

- Package root: `com.pvpindex.bans`
- 4-space indentation, 120-char line limit, no wildcard imports
- DTOs are Java `record`s where immutable (all API response types, `LocalBan`)
- All API calls return `Optional<T>` - never throw on network errors
- SQLite writes are synchronous; API calls run on separate threads
- `StorageManager` owns the `Connection`; pass it to `BanDao` - no connection pool needed for single-threaded SQLite writes
- Thread safety: `BanSync.syncRunning` flag prevents concurrent sync cycles; all SQLite ops are on the BanSync thread or the Paper async login thread
