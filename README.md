# GlobalBoosters - Server-Wide Buffs, Events & Economy for Minecraft 1.21+

> Activate server-wide boosters for XP, Drops, Flight, Effects and more. Built-in GUI Shop, Supply System, Scheduled Events, Booster Queue, PlaceholderAPI support and full HEX/Gradient customization.

![Java](https://img.shields.io/badge/Java-21-orange) ![Spigot](https://img.shields.io/badge/Spigot-1.21+-yellow) ![License](https://img.shields.io/badge/License-MIT-blue) ![PlaceholderAPI](https://img.shields.io/badge/PlaceholderAPI-Supported-green)

---

## What is GlobalBoosters?

GlobalBoosters is a Minecraft Spigot plugin that lets server owners create a complete booster ecosystem. Players can purchase, activate and benefit from server-wide buffs that affect every online player at once. Unlike basic multiplier plugins, GlobalBoosters combines an economy-driven shop, scarcity mechanics, automated scheduling and a full visual feedback system into one lightweight package.

Whether you run a survival server, an MMO-style network or a minigame hub, GlobalBoosters gives your community a reason to stay online, engage with the economy and participate in server events.

---

## Features

### 25+ Booster Types

GlobalBoosters ships with over 25 unique booster types divided into three categories.

**Progression Boosters** increase player advancement speed. This includes XP Multiplier, Mining Speed, Fishing Luck, Farming Fortune, Mob Drop multiplier, Combat Damage boost, Spawner Rate increase and Plant Growth acceleration.

**Gameplay Boosters** change how the game feels for everyone. Fly gives all players creative flight. No Fall Damage, Keep Inventory, Hunger Saver and Armor Durability all provide quality-of-life improvements. Effect boosters like Haste, Speed, Strength, Regeneration, Night Vision, Fire Resistance, Resistance and Jump Boost apply potion effects server-wide.

**Negative Curses** are optional debuff boosters for special events. Slowness, Mining Fatigue, Weakness, Poison, Wither, Blindness, Hunger and Nausea can be used to create challenge events or as punishment mechanics. All negative effects can be toggled off globally in the config.

Every booster type has configurable price, duration and multiplier. Effect boosters automatically apply and remove potion effects as players join and leave.

---

### GUI Shop with Vault Integration

GlobalBoosters includes a built-in chest GUI shop where players can browse and purchase boosters using their server economy. The shop requires Vault and any compatible economy plugin (EssentialsX, CMI, etc.).

Each booster displays its name, price, duration, multiplier and a description of its effects. The shop supports a confirmation screen to prevent accidental purchases. Purchased boosters are given as physical items that players right-click to activate.

```yaml
boosters:
  exp_multiplier:
    enabled: true
    price: 1000.0
    duration: 30
    multiplier: 2.0
    bossbar_color: YELLOW
  fly:
    enabled: true
    price: 2500.0
    duration: 30
    bossbar_color: BLUE
```

---

### Limited Supply System

The Supply System adds scarcity to your booster economy. When enabled, each booster type has a maximum number of purchases per reset cycle. Once the supply runs out, players must wait for the next restock.

This creates demand, encourages players to log in at restock time and prevents inflation from unlimited booster access. The restock hour is configurable and the server announces restocks automatically.

```yaml
limited_supply_mode: true
max_supply_per_booster: 10
supply_reset_hour: 0
```

---

### Scheduled Boosters

Automate booster activations with two scheduling systems.

**Fixed Schedules** let you define specific boosters for specific days and times. Set up a Double XP Weekend, a Friday Night Speed Boost or a daily Happy Hour. Each schedule supports day-of-week filtering, custom activator names and independent durations.

**Random Events** activate random boosters from a configurable pool at set intervals. This keeps the server dynamic and gives players a reason to stay online for surprise events.

```yaml
scheduled_boosters:
  enabled: true
  timezone: "Europe/Rome"
  schedules:
    exp_weekend:
      enabled: true
      type: EXP_MULTIPLIER
      hour: 20
      minute: 0
      duration: 60
      activator_name: "Server"
      days:
        - FRIDAY
        - SATURDAY
        - SUNDAY

random_scheduled_boosters:
  enabled: true
  interval_minutes: 30
  duration_minutes: 5
  activator_name: "Server"
  booster_pool:
    - EXP_MULTIPLIER
    - SPEED
    - FLY
```

---

### BossBar and ActionBar Display

Active boosters are displayed as BossBars at the top of the screen showing the booster name, remaining time, multiplier and who activated it. Each booster type can have its own BossBar color configured individually.

For servers that prefer a lighter approach, the ActionBar display shows active booster info at the bottom of the screen. Both systems can run simultaneously or independently. Both can be toggled on or off in the config without restarting the server.

```yaml
bossbar_enabled: true
actionbar_enabled: false

boosters:
  exp_multiplier:
    bossbar_color: YELLOW  # PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
```

---

### Booster Queue System

When the queue is enabled, activating a booster that is already running will not be rejected. Instead, it gets placed in a queue and automatically activates once the current one expires. Players are notified of their queue position and the server announces when queued boosters start.

This is especially useful for servers with an active economy where multiple players might purchase the same booster type.

```yaml
booster_queue_enabled: true
```

---

### Booster History Log

Track every booster activation, expiration and queue event in a plain text log file. Each entry records the timestamp, activator name, booster type, duration and source (Player Item, Console, Scheduled, Random or Queue). The log is saved to `plugins/GlobalBoosters/logs/booster_history.txt`.

```yaml
history_log_enabled: true
```

Example log output:
```
[2025-02-15 14:30:12] ACTIVATED | Type: EXP_MULTIPLIER | By: Steve | Duration: 30min | Source: Player Item
[2025-02-15 15:00:12] EXPIRED   | Type: EXP_MULTIPLIER | Reason: Expired
[2025-02-15 15:00:13] ACTIVATED | Type: EXP_MULTIPLIER | By: Alex | Duration: 30min | Source: Queue (Player Item)
```

---

### PlaceholderAPI Integration

GlobalBoosters registers a PlaceholderAPI expansion automatically when PAPI is installed. No manual downloads or `/papi ecloud` commands needed. Use these placeholders in scoreboards, tab lists, holograms and any plugin that supports PlaceholderAPI.

**General Placeholders**

| Placeholder | Output |
|---|---|
| `%globalboosters_active_count%` | Number of active boosters |
| `%globalboosters_active_list%` | Comma-separated active booster names |
| `%globalboosters_max_active%` | Max allowed active boosters |

**Per-Booster Placeholders** (replace `<type>` with the booster name, e.g. `exp_multiplier`, `fly`, `speed`)

| Placeholder | Output |
|---|---|
| `%globalboosters_<type>_active%` | `true` or `false` |
| `%globalboosters_<type>_time%` | `12m 30s` |
| `%globalboosters_<type>_time_seconds%` | `750` |
| `%globalboosters_<type>_multiplier%` | `2.0` |
| `%globalboosters_<type>_activator%` | `Steve` |

**Scoreboard Example**
```
Active Boosters: %globalboosters_active_count%
XP Boost: %globalboosters_exp_multiplier_time%
Fly: %globalboosters_fly_active%
```

**Hologram Example**
```
Server Boosters: %globalboosters_active_count%/%globalboosters_max_active%
EXP Multiplier: %globalboosters_exp_multiplier_multiplier%x
Activated by: %globalboosters_exp_multiplier_activator%
```

---

### Full HEX and Gradient Color Support

Every message in the plugin supports HEX colors and gradient formatting. Booster names, chat announcements, BossBar titles, shop GUI text and lore lines are all fully customizable in `messages.yml`.

```yaml
booster-names:
  exp_multiplier: "<gradient:#FFD700:#FFA500>&lExperience Booster</gradient>"
  fly: "<gradient:#87CEEB:#00BFFF>&lFlight Booster</gradient>"
```

---

### Sound Control

All plugin sounds can be globally enabled or disabled and the volume can be adjusted independently.

```yaml
sounds_enabled: true
sound_volume: 1.0
```

---

## Commands

| Command | Description | Permission |
|---|---|---|
| `/boostshop` | Open the booster shop GUI | `globalboosters.shop` |
| `/globalboosters help` | Show the help menu | - |
| `/globalboosters list` | List all active boosters | - |
| `/globalboosters reload` | Reload config and messages | `globalboosters.admin.reload` |
| `/booster give <player> <type> [duration]` | Give a booster item to a player | `globalboosters.admin.give` |
| `/booster start <type> <duration> [activator]` | Force-start a booster from console or in-game | `globalboosters.admin` |
| `/booster stop <type>` | Force-stop an active booster | `globalboosters.admin` |
| `/booster reload` | Reload config and messages | `globalboosters.admin.reload` |
| `/booster stats` | View top 10 most used boosters | `globalboosters.admin.stats` |
| `/booster schedule` | View configured scheduled boosters | `globalboosters.admin` |
| `/booster random` | View random booster pool info | `globalboosters.admin` |

**Aliases:** `/booster` can also be used as `/boost` or `/gb`. `/globalboosters` can also be used as `/gbooster` or `/globalboost`. `/boostshop` can also be used as `/bs`.

---

## Permissions

| Permission | Description | Default |
|---|---|---|
| `globalboosters.shop` | Access the booster shop | Everyone |
| `globalboosters.use` | Use booster items | Everyone |
| `globalboosters.use.*` | Use all booster types | OP |
| `globalboosters.use.<type>` | Use a specific booster type | OP |
| `globalboosters.admin` | Admin commands (start, stop, schedule, random) | OP |
| `globalboosters.admin.give` | Give boosters to players | OP |
| `globalboosters.admin.reload` | Reload configuration | OP |
| `globalboosters.admin.stats` | View booster statistics | OP |
| `globalboosters.fly.bypass` | Keep flight when fly booster expires | OP |

---

## Installation

1. Download the latest GlobalBoosters jar file.
2. Place it in your server's `plugins` folder.
3. Install [Vault](https://www.spigotmc.org/resources/vault.34315/) and an economy plugin if you want to use the shop.
4. Optionally install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) for placeholder support.
5. Start or restart your server.
6. Edit `config.yml` and `messages.yml` in `plugins/GlobalBoosters/` to your liking.
7. Run `/booster reload` to apply changes.

---

## Requirements

| Dependency | Required | Purpose |
|---|---|---|
| Spigot / Paper 1.21+ | Yes | Server software |
| Java 21+ | Yes | Runtime |
| Vault | Only if shop is enabled | Economy integration |
| PlaceholderAPI | No | Placeholder support for third-party plugins |

---

## Configuration Files

| File | Purpose |
|---|---|
| `config.yml` | All plugin settings, booster prices, durations, multipliers, schedules, toggles |
| `messages.yml` | Every message, booster name, GUI text, announcement format |
| `active_boosters.db` | SQLite database for persistent active boosters across restarts |
| `supply_data.db` | SQLite database for supply tracking |
| `logs/booster_history.txt` | Activation history log (when enabled) |

---

## Frequently Asked Questions

**Do boosters persist after a server restart?**
Yes. Active boosters are saved to an SQLite database and restored on startup with correct remaining time.

**Can I use this without Vault?**
Yes. Set `shop_gui_enabled: false` in config.yml and use the `/booster give` and `/booster start` commands instead. Vault is only needed for the shop.

**Can I disable negative effect boosters?**
Yes. Set `negative_effects_enabled: false` in config.yml and all curse-type boosters will be disabled globally.

**Do effects apply to players who join mid-booster?**
Yes. When a player joins, all active potion effects and flight are applied automatically with the correct remaining duration.

**Can I have BossBar and ActionBar at the same time?**
Yes. Both systems are independent. Enable both in config.yml if you want dual feedback.

**What happens if a player activates a booster that is already running?**
If `booster_queue_enabled` is true, it gets queued and activates automatically when the current one expires. If false, the activation is rejected.

**Does PlaceholderAPI require extra setup?**
No. If PlaceholderAPI is installed on your server, GlobalBoosters registers its expansion automatically on startup. No ecloud downloads or extra commands needed.

---

## Support

Found a bug or have a feature request? Open an issue on this repository.

---

## License

This project is licensed under the MIT License. See `LICENSE` for details.
