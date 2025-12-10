# GlobalBoosters-Fork

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
    - **Item-based economy** (trade on Booster-Coins)

## 📦 Economy Providers

The plugin now supports multiple economy systems:

### CoinsEngine (supported many currencies)
```yaml
economy:
  coins_engine:
    enabled: true
    currencies:
      gems:
        price_multiplier: 0.01
      coins:
        price_multiplier: 0.05
```

### Vault
```yaml
economy:
  vault:
    enabled: true
    item: KELP
    price_multiplier: 1.0 # How much should the price increase for this type of currency
```

### Shop as Booster-Coins (Get for exchanging items)
```yml
economy:
  booster_coins:
    enabled: true
    item: GOLD_NUGGET
    name: "<gradient:#FFD700:#FFA500>Booster-Coin</gradient>"
    price_multiplier: 0.001
    exchange_rates:
      DIAMOND: 32 # How many items need to sell for get one coin
      SKELETON_SKULL: 1
      WITHER_SKELETON_SKULL: 1
      CREEPER_HEAD: 1
      ZOMBIE_HEAD: 1
      PLAYER_HEAD: 1
      PIGLIN_HEAD: 1
      DRAGON_HEAD: 1
```

## Dependencies
- Required: Paper 1.21.10

- Optional:
  1. Vault for traditional economy
  2. CoinsEngine for coins support

## 🔄 Original Features

All original features from the base Global-Boosters plugin are preserved.

**Note:** This is a community fork with additional features. For the original plugin, visit **lino9999/Global-Boosters**.
