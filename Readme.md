# Global Boosters

Fork of the original [Global-Boosters](https://github.com/lino9999/Global-Boosters) plugin with additional features and improvements.

## 🚀 New Features
### 1. MiniMessages formats
- `messages.yml` supported all *MiniMessages* formats (https://docs.papermc.io/adventure/minimessage/format/)

### 2. CoinsEngine Support
- **Full integration** with [CoinsEngine](https://github.com/nulli0n/CoinsEngine-spigot)
- Purchase boosters using CoinsEngine currency
- Flexible economy system supporting multiple providers:
    - **Vault** (traditional economy)
    - **CoinsEngine** (custom coins system)

## 📦 Economy Providers

The plugin now supports multiple economy systems:

### CoinsEngine
```yaml
economy:
  # If coins_engine enabled - use CoinsEngine currency against Vault
  coins_engine:
    enabled: true
    currency: coins
```

### Vault
```yaml
economy:
  # If coins_engine enabled - use CoinsEngine currency against Vault
  coins_engine:
    enabled: false
    currency: coins
```

## Dependencies
- Required: Paper 1.21.8

- Optional:
  1. Vault for traditional economy
  2. CoinsEngine for coins support

## 🔄 Original Features

All original features from the base Global-Boosters plugin are preserved.

**Note:** This is a community fork with additional features. For the original plugin, visit **lino9999/Global-Boosters**.
