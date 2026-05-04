# PvPIndex MCBans

A Minecraft Paper plugin that enforces bans through the [PvPIndex](https://pvpindex.com) REST API, with full offline fallback via a local SQLite cache.

## Features

- **Global & local bans** — synced to the PvPIndex API in real time
- **Temp bans** — with expiry times, auto-expired locally if the API is unavailable
- **Offline resilience** — bans issued while the API is down are queued and pushed at the next sync cycle
- **Background delta sync** — downloads ban changes from the API on a configurable interval (default 60 min)
- **API-first, cache-fallback** — login checks hit the API first (3 s timeout), then fall back to SQLite

## Requirements

| Component | Version |
|-----------|---------|
| Java | 21+ |
| Paper | 1.21+ |
| PvPIndex API key | [Apply here](https://pvpindex.com/apply) |

## Installation

1. Drop `pvpindex-mcbans-*.jar` into your server's `plugins/` folder.
2. Start the server once to generate `plugins/MCBans/config.yml`.
3. Set your API key:
   ```yaml
   pvpindex:
     apiKey: "your-bearer-token-here"
   ```
4. Restart or `/mcbans reload`.

## Configuration (`config.yml`)

```yaml
pvpindex:
  apiUrl:       https://api.pvpindex.com   # API base URL
  apiKey:       ""                          # Bearer token from PvPIndex dashboard
  syncInterval: 60                          # Delta-sync interval in minutes

prefix:   "&cMCBans &8>&r "
language: default
permission: SuperPerms   # Vault | SuperPerms | OPs

defaultLocal: "You have been banned!"
defaultTemp:  "You have been temporarily banned!"
defaultKick:  "You have been kicked!"

isDebug:   false
logEnable: false
logFile:   "plugins/MCBans/actions.log"

failsafe: false   # Deny login if API AND local cache both say unknown
```

## Commands

| Command | Aliases | Permission | Description |
|---------|---------|------------|-------------|
| `/ban <player> [reason]` | | `mcbans.ban.local` | Local ban |
| `/ban <player> g [reason]` | `/gban` | `mcbans.ban.global` | Global ban |
| `/ban <player> t <n> <m\|h\|d\|w> [reason]` | `/tban` | `mcbans.ban.temp` | Temp ban |
| `/unban <player\|IP\|UUID>` | | `mcbans.unban` | Unban |
| `/kick <player> [reason]` | | `mcbans.kick` | Kick |
| `/banip <ip> [reason]` | `/ipban` | `mcbans.ban.ip` | IP ban |
| `/lookup <player\|UUID>` | `/lup` | `mcbans.lookup.player` | Player history |
| `/banlookup <id>` | `/blup` | `mcbans.lookup.ban` | Ban details |
| `/altlookup <player\|UUID>` | `/alup` | `mcbans.lookup.alt` | Alt accounts |
| `/mcbans [reload\|sync\|help]` | | `mcbans.admin` | Admin commands |

## Permissions

| Permission | Default | Description |
|-----------|---------|-------------|
| `mcbans.admin` | op | Full admin access |
| `mcbans.ban.global` | op | Issue global bans |
| `mcbans.ban.local` | op | Issue local bans |
| `mcbans.ban.temp` | op | Issue temp bans |
| `mcbans.ban.ip` | op | Ban IPs |
| `mcbans.ban.exempt` | op | Exempt from bans |
| `mcbans.unban` | op | Unban players |
| `mcbans.kick` | op | Kick players |
| `mcbans.kick.exempt` | op | Exempt from kicks |
| `mcbans.view.bans` | op | See ban info on join |
| `mcbans.view.alts` | op | See alt alerts on join |
| `mcbans.lookup.player` | op | Use /lookup |
| `mcbans.lookup.ban` | op | Use /banlookup |
| `mcbans.lookup.alt` | op | Use /altlookup |
| `mcbans.announce` | op | Receive broadcast announcements |

## Architecture

```
Player Login (AsyncPlayerPreLoginEvent)
  └── PvPIndexApiClient.getBanStatus(uuid)   [3 s timeout]
        ├── API available → upsert to SQLite, allow/deny login
        └── API timeout  → fallback BanDao.findActiveBan(uuid)

/ban command
  └── Ban.run()
        ├── BanDao.insertOfflineBan(...)      [immediate, always]
        └── PvPIndexApiClient.ban(...)        [best-effort, async]
              ├── success → BanDao.markSynced(uuid)
              └── failure → left in is_synced=0 queue

BanSync thread (background, every syncInterval minutes)
  ├── uploadUnsynced()   — push is_synced=0 bans to API
  └── downloadDelta()    — pull bans updated since lastSyncAt
```

## Development

```bash
# Build fat JAR
mvn clean package

# Checkstyle only
mvn checkstyle:check

# Run tests
mvn test

# Full verify (checkstyle + compile + test + coverage report)
mvn verify
```

Coverage reports are generated at `target/site/jacoco/index.html`.

## CI / CD

GitHub Actions runs on every push and PR:

| Job | Trigger | What it does |
|-----|---------|-------------|
| `checkstyle` | push / PR | Runs `mvn checkstyle:check` |
| `test` | after checkstyle passes | Compiles, runs JUnit 5 tests, uploads coverage to Codecov |

Set the `CODECOV_TOKEN` secret in your GitHub repository settings to enable coverage reporting.

## Changelog

See [CHANGELOG.txt](CHANGELOG.txt).

## License

Distributed under the terms of the original MCBans license. See [LICENSE](LICENSE).
