# 🚀 GlobalBoosters - Server-Wide Buffs & Events (1.21+)

> **Engage your community with powerful global effects.**
> Activate server-wide boosters for XP, Drops, Flight, and more. Includes a **Supply System** to create scarcity and **Scheduled Events** for automation.

![Java](https://img.shields.io/badge/Java-21-orange) ![Spigot](https://img.shields.io/badge/API-1.21-yellow) ![License](https://img.shields.io/badge/License-MIT-blue)

---

## ⚡ Why GlobalBoosters?
Unlike simple multiplier plugins, **GlobalBoosters** offers a complete event ecosystem. You can sell boosters in a GUI, limit their daily supply to create demand (FOMO), or schedule "Happy Hours" automatically. It supports both **Positive Buffs** and **Negative Curses** for unique server events.

### 🔥 Key Features

* **🌟 25+ Unique Booster Types**
    * **Progression:** XP Multiplier, Jobs/McMMO (via commands), Skill Boosts.
    * **Gameplay:** Fly, No Fall Damage, Keep Inventory, Hunger Saver.
    * **Economy:** Mob Drops, Mining Speed, Fishing Luck, Spawner Rates.

* **📅 Advanced Scheduling System**
    * **Fixed Schedules:** Set up specific boosters for weekends or holidays (e.g., "Double XP Weekend").
    * **Random Events:** Automatically activate random boosters at set intervals to keep players online.

* **🛒 Supply & Economy Shop**
    * Built-in **GUI Shop** with Vault support.
    * **Limited Supply Mode:** Limit how many boosters can be bought per day/week to control the economy.

* **📢 Visual Feedback**
    * Automatic **BossBars** showing active boosters, duration, and the activator's name.
    * Fully customizable messages with HEX/Gradient support.

---

## ⚙️ Configuration
Easily configure prices, durations, and multipliers in `config.yml`.

```yaml
boosters:
  exp_multiplier:
    enabled: true
    price: 1000.0
    duration: 30 # Minutes
    multiplier: 2.0 # 2x XP

  fly:
    enabled: true
    price: 2500.0
    duration: 30 # Gives /fly to everyone!
