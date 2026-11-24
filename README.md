# tr237_Playtime_System

A lightweight, high-precision Playtime Tracking plugin built with Java 21 for **Paper/Spigot 1.21.11** (`tr237`).

## Features
- **Accurate Session Tracking**: Tracks online playtime using timestamp deltas with zero per-tick overhead.
- **SQLite Persistence**: Asynchronous periodic auto-saves and synchronous shutdown flushes prevent data loss on restarts.
- **Offline Player Support**: Query and modify playtime for offline players via cached UUID lookups.
- **Top 10 Leaderboard**: Live ranking combining stored and active session playtime.
- **Flexible Duration Parsing**: Supports formats such as `1d2h30m`, `45m`, `10h30s`, or raw seconds.

## Commands
| Command | Permission | Description |
| :--- | :--- | :--- |
| `/playtime` | `playtime.use` | Check your own total playtime |
| `/playtime <player>` | `playtime.others` | Check an online or offline player's playtime |
| `/playtime top` | `playtime.top` | Display the top 10 playtime leaderboard |
| `/playtime set <player> <duration>` | `playtime.admin` | Set a player's total playtime |
| `/playtime add <player> <duration>` | `playtime.admin` | Add duration to a player's playtime |
| `/playtime remove <player> <duration>` | `playtime.admin` | Subtract duration from a player's playtime |
| `/playtime reload` | `playtime.reload` | Reload `config.yml` and auto-save interval |

## Build
```bash
mvn clean package