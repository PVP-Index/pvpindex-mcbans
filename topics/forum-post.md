> **This is a refactored fork of the original [MCBans](https://dev.bukkit.org/projects/mcbans) plugin.** The ban-sharing concept, command structure, and permission layout originate from the MCBans team and their contributors. This fork modernises the codebase for Java 21 / Paper 1.21+ and replaces the legacy MCBans backend with the PvPIndex REST API.

**PvPIndex MCBans** connects your Paper server to the [mcbans.pvpindex.com](https://mcbans.pvpindex.com) global ban network. Players banned on any participating server are automatically blocked everywhere else - while you keep full control over local bans, storage backends, and permission levels.

---

## Features

- **Global & local bans** - issue a local ban (stays on your server) or a global ban synced to all connected servers in real time
- **Temporary bans** - time-limited with auto-expiry in minutes, hours, days, or weeks
- **IP bans** - ban by IP address to block VPN abuse and ban evasion
- **VPN detection** - AntVPN WebSocket integration; configurable actions (`WARN`, `KICK`, `BAN`); `mcbans.vpn.bypass` permission lets trusted players skip the check; disabled by default via `antivpn.yml`
- **Legacy ban import** - checks mcbans.com for existing bans on player join; results cached locally; controlled by the `pvpindex.legacy` config section
- **Public kicks** - `pvpindex.kicks.public` flag reports kicks to the API and shows them on bans.pvpindex.com
- **Reason presets** - define shorthand keys in config (`#hacks`, `#toxicity`) that expand to full reason strings; extended form supports a `default-duration` for temp bans; case-insensitive with tab-completion
- **Customizable kick messages** - per-type templates (`global`, `local`, `temp`, `failsafe`) with `{reason}`, `{admin}`, `{expires}`, and `{appeal_url}` placeholders
- **Offline resilience** - bans issued during API outages are queued and pushed on recovery; `failsafe` mode can block all logins when the API is unreachable
- **Delta sync** - downloads only ban changes on a configurable interval (default 60 min)
- **Multiple storage backends** - SQLite (default, zero-config), MySQL/MariaDB, PostgreSQL
- **Alt-account detection** - `/altlookup` surfaces known alternate accounts via the PvPIndex API
- **Developer API (JitPack)** - lightweight `pvpindex-mcbans-api` module lets other plugins query ban status without a Bukkit dependency
- **Vault integration** - optional; swap the permission backend in one config line
- **Fully translatable** - swap the `language` key to load a different locale file

---

## Requirements

| Component | Version |
|-----------|---------|
| Java | 21+ |
| Paper | 1.21+ |
| PvPIndex API key | Free at [pvpindex.com/apply](https://pvpindex.com/apply) |
| Vault *(optional)* | Any recent version |

---

## Installation

1. Download `pvpindex-mcbans-<version>.jar` from [GitHub Releases](https://github.com/PVP-Index/pvpindex-mcbans/releases).
2. Drop the JAR into `plugins/` and start the server - `plugins/MCBans/config.yml` is generated automatically.
3. [Apply for an API key](https://pvpindex.com/apply) and paste it into config:

```yaml
pvpindex:
  apiKey: "your-bearer-token-here"
```

4. Reload with `/mcbans reload` or restart.
5. Run `/mcbans sync` to pull the current ban list from the network.

→ Full guide: [docs.pvpindex.com/mcbans/installation](https://docs.pvpindex.com/mcbans/installation)

---

## Commands

### Ban commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/ban <player> [reason]` | `mcbans.ban.local` | Issue a local ban |
| `/ban <player> g <reason>` · `/gban` | `mcbans.ban.global` | Issue a global ban (synced network-wide) |
| `/ban <player> t <n> <m\|h\|d\|w> [reason]` · `/tban` | `mcbans.ban.temp` | Temporary ban with auto-expiry |
| `/banip <ip> [reason]` · `/ipban` | `mcbans.ban.ip` | Ban an IP address |
| `/unban <player\|IP\|UUID>` | `mcbans.unban` | Remove any active ban |

### Moderation commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/kick <player> [reason]` | `mcbans.kick` | Kick without banning |
| `/lookup <player\|UUID>` · `/lup` | `mcbans.lookup.player` | View ban history |
| `/banlookup <id>` · `/blup` | `mcbans.lookup.ban` | View a ban record by ID |
| `/altlookup <player\|UUID>` · `/alup` | `mcbans.lookup.alt` | Show known alternate accounts |

### Admin commands

| Command | Description |
|---------|-------------|
| `/mcbans` | Show help |
| `/mcbans reload` | Reload config and language files |
| `/mcbans sync` | Force an immediate push + download cycle |
| `/mcbans presets` | List all configured reason presets (`mcbans.ban.local`) |

---

## Configuration

```yaml
pvpindex:
  apiUrl:       https://api.pvpindex.com
  apiKey:       ""            # ← paste your key here
  syncInterval: 60            # background sync in minutes

storage:
  backend: sqlite             # sqlite | mysql | postgresql

language:   default
permission: SuperPerms        # SuperPerms | Vault

failsafe: false               # true = block all logins when API unreachable

# Optional: customise the message shown to banned/kicked players
kick-message:
  global:   "&cYou are globally banned.\n&7Reason: {reason}\n&7Appeal: {appeal_url}"
  local:    "&cYou are banned from this server.\n&7Reason: {reason}"
  temp:     "&cYou are temporarily banned until {expires}.\n&7Reason: {reason}"
  failsafe: "&cUnable to verify your ban status. Try again later."

# Optional: shorthand reason keys usable in all ban commands
reason-presets:
  hacks:
    reason: "Hacking / using unauthorized modifications"
  toxicity:
    reason: "Toxic behaviour"
  farming:
    reason: "Ban/ELO farming"
    default-duration: "7d"
```

VPN detection is configured separately in `plugins/MCBans/antivpn.yml`:

```yaml
antivpn:
  enabled: false          # set to true to enable VPN detection
  action: KICK            # WARN | KICK | BAN
  token: ""               # your AntVPN API token
```

→ Full reference: [docs.pvpindex.com/mcbans/configuration](https://docs.pvpindex.com/mcbans/configuration)

---

## Developer API (JitPack)

Add the lightweight API client to your own plugin - no Bukkit dependency required:

```xml
<repositories>
    <repository>
        <id>jitpack</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
<dependency>
    <groupId>com.github.PVP-Index.pvpindex-mcbans</groupId>
    <artifactId>pvpindex-mcbans-api</artifactId>
    <version>1.2.0</version>
</dependency>
```

→ [Developer API documentation](https://docs.pvpindex.com/mcbans/developer-api)

---

## Links

- **Ban registry** - [mcbans.pvpindex.com](https://mcbans.pvpindex.com)
- **Documentation** - [docs.pvpindex.com/mcbans](https://docs.pvpindex.com/mcbans/overview)
- **Source** - [github.com/PVP-Index/pvpindex-mcbans](https://github.com/PVP-Index/pvpindex-mcbans)
- **Issue tracker** - [github.com/PVP-Index/pvpindex-mcbans/issues](https://github.com/PVP-Index/pvpindex-mcbans/issues)
- **Apply for an API key** - [pvpindex.com/apply](https://pvpindex.com/apply)

---

## Credits

PvPIndex MCBans is a refactored fork of the original **MCBans** plugin by the MCBans team and contributors ([dev.bukkit.org/projects/mcbans](https://dev.bukkit.org/projects/mcbans)). The ban-sharing concept, TCP wire protocol, command set, and permission layout all originate from that project. This fork modernises the codebase for Java 21 / Paper 1.21+ and replaces the legacy MCBans backend with the PvPIndex REST API.
