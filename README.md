# PvPIndex MCBans

> **Fork notice:** PvPIndex MCBans is a refactored fork of the original [MCBans](https://dev.bukkit.org/projects/mcbans) plugin by the MCBans team. The core ban-sharing concept, TCP protocol, command structure, and permission layout originate from that project. We have modernised the codebase for Java 21 and Paper 1.21+, replaced the legacy MCBans API backend with the PvPIndex REST API, split the project into a multi-module Maven build, and published the API client module on JitPack. Full credit to the original MCBans authors and contributors.

Ban management plugin for Paper 1.21 servers. Connects your server to the [PvPIndex](https://pvpindex.com) global ban network so that players banned on one participating server are automatically blocked everywhere else — while still giving you complete control over local bans and storage.

## Download

Get the latest JAR from the [GitHub Releases](https://github.com/PVP-Index/pvpindex-mcbans/releases) page.

## Quick start

1. Drop `pvpindex-mcbans-<version>.jar` into your server's `plugins/` folder.
2. Restart the server — `plugins/MCBans/config.yml` is generated with defaults.
3. [Apply for an API key](https://pvpindex.com/apply), then set it:
   ```yaml
   pvpindex:
     apiKey: "your-bearer-token-here"
   ```
4. Run `/mcbans reload` or restart.

## Requirements

| Component | Version |
|-----------|---------|
| Java | 21+ |
| Paper | 1.21+ |
| PvPIndex API key | [pvpindex.com/apply](https://pvpindex.com/apply) |

Optional: [Vault](https://dev.bukkit.org/projects/vault) for non-OP permission integration.

## Features

- **Global & local bans** — synced to the PvPIndex network in real time
- **Temp bans** — time-limited with auto-expiry
- **Offline resilience** — bans issued during API outages are queued and pushed on recovery
- **Delta sync** — downloads ban changes on a configurable background interval (default 60 min)
- **Multiple storage backends** — SQLite (default), MySQL/MariaDB, PostgreSQL
- **Developer API** — lightweight [JitPack client](https://mcbans.pvpindex.com) for other plugins to query ban status without depending on MCBans being installed

## Documentation

Full documentation is available at **[docs.pvpindex.com/mcbans](https://docs.pvpindex.com/mcbans/overview)**:

- [Overview](https://docs.pvpindex.com/mcbans/overview)
- [Installation](https://docs.pvpindex.com/mcbans/installation)
- [Configuration](https://docs.pvpindex.com/mcbans/configuration)
- [Commands](https://docs.pvpindex.com/mcbans/commands)
- [Permissions](https://docs.pvpindex.com/mcbans/permissions)
- [Developer API](https://docs.pvpindex.com/mcbans/developer-api)

## Building

```bash
# Full build + tests
mvn verify

# Plugin JAR only (skips tests)
mvn package -DskipTests

# Checkstyle only
mvn checkstyle:check
```

The fat JAR is output to `pvpindex-mcbans-plugin/target/pvpindex-mcbans-<version>.jar`.

## Developer API (JitPack)

Add the lightweight API client to your own plugin — no Bukkit dependency required:

```xml
<!-- Maven -->
<repositories>
    <repository>
        <id>jitpack</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
<dependency>
    <groupId>com.github.PVP-Index.pvpindex-mcbans</groupId>
    <artifactId>pvpindex-mcbans-api</artifactId>
    <version>1.0.0</version>
</dependency>
```

See [Developer API docs](https://docs.pvpindex.com/mcbans/developer-api) for usage examples.

## Credits

PvPIndex MCBans is a refactored fork of the original **MCBans** plugin. Original project: <https://dev.bukkit.org/projects/mcbans>. The ban-sharing concept, TCP wire protocol, command set, and permission layout all originate from the MCBans team and their contributors. This fork modernises the codebase and replaces the legacy MCBans backend with the PvPIndex REST API.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## License

Distributed under the terms of the original MCBans license. See [LICENSE](LICENSE).

