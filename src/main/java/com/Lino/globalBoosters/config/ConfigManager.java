package com.Lino.globalBoosters.config;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.BoosterType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.DayOfWeek;
import java.util.*;

public class ConfigManager {

    private final GlobalBoosters plugin;
    private FileConfiguration config;
    private final Map<BoosterType, Double> boosterPrices;
    private final Map<BoosterType, Integer> boosterDurations;
    private final Map<BoosterType, Double> boosterMultipliers;
    private final Map<BoosterType, Boolean> boosterEnabled;
    private final List<ScheduledBooster> scheduledBoosters;
    private int maxActiveBoosters;
    private boolean limitedSupplyEnabled;
    private int resetHour;
    private int maxSupplyPerBooster;
    private boolean keepEffectsOnDeath;
    private boolean scheduledBoostersEnabled;
    private String scheduledBoostersTimezone;
    private boolean shopGuiEnabled;
    private boolean showActivatorName;
    private boolean negativeEffectsEnabled;
    private boolean randomScheduledEnabled;
    private int randomScheduledInterval;
    private int randomScheduledDuration;
    private String randomScheduledActivatorName;
    private List<String> randomScheduledBoosters;
    private double soundVolume;

    public ConfigManager(GlobalBoosters plugin) {
        this.plugin = plugin;
        this.boosterPrices = new HashMap<>();
        this.boosterDurations = new HashMap<>();
        this.boosterMultipliers = new HashMap<>();
        this.boosterEnabled = new HashMap<>();
        this.scheduledBoosters = new ArrayList<>();
        this.randomScheduledBoosters = new ArrayList<>();

        loadConfig();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        maxActiveBoosters = config.getInt("max_active_boosters", 3);
        limitedSupplyEnabled = config.getBoolean("limited_supply_mode", false);
        resetHour = config.getInt("supply_reset_hour", 0);
        maxSupplyPerBooster = config.getInt("max_supply_per_booster", 10);
        keepEffectsOnDeath = config.getBoolean("keep_effects_on_death", true);
        shopGuiEnabled = config.getBoolean("shop_gui_enabled", true);
        showActivatorName = config.getBoolean("show_activator_name", true);
        negativeEffectsEnabled = config.getBoolean("negative_effects_enabled", true);
        soundVolume = config.getDouble("sound_volume", 1.0);

        scheduledBoostersEnabled = config.getBoolean("scheduled_boosters.enabled", false);
        scheduledBoostersTimezone = config.getString("scheduled_boosters.timezone", "UTC");
        loadScheduledBoosters(config);

        randomScheduledEnabled = config.getBoolean("random_scheduled_boosters.enabled", false);
        randomScheduledInterval = config.getInt("random_scheduled_boosters.interval_minutes", 60);
        randomScheduledDuration = config.getInt("random_scheduled_boosters.duration_minutes", 30);
        randomScheduledActivatorName = config.getString("random_scheduled_boosters.activator_name", "Lucky Draw");
        randomScheduledBoosters = config.getStringList("random_scheduled_boosters.booster_pool");

        if (randomScheduledBoosters.isEmpty() && randomScheduledEnabled) {
            for (BoosterType type : BoosterType.values()) {
                if (!type.isNegativeEffect()) {
                    randomScheduledBoosters.add(type.name());
                }
            }
            config.set("random_scheduled_boosters.booster_pool", randomScheduledBoosters);
        }

        for (BoosterType type : BoosterType.values()) {
            String path = "boosters."
                    + type.name().toLowerCase();

            if (!config.contains(path + ".enabled")) {
                config.set(path + ".enabled", true);
            }
            if (!config.contains(path + ".price")) {
                config.set(path + ".price", getDefaultPrice(type));
            }
            if (!config.contains(path + ".duration")) {
                config.set(path + ".duration", 30);
            }

            boolean enabled = config.getBoolean(path + ".enabled", true);
            if (type.isNegativeEffect() && !negativeEffectsEnabled) {
                enabled = false;
            }

            boosterEnabled.put(type, enabled);
            boosterPrices.put(type, config.getDouble(path + ".price"));
            boosterDurations.put(type, config.getInt(path + ".duration"));

            if (!type.isEffectBooster() && !isNoMultiplierBooster(type)) {
                if (!config.contains(path + ".multiplier")) {
                    config.set(path + ".multiplier", type.getDefaultMultiplier());
                }
                boosterMultipliers.put(type, config.getDouble(path + ".multiplier"));
            }
        }

        plugin.saveConfig();
    }

    private void loadScheduledBoosters(FileConfiguration config) {
        scheduledBoosters.clear();
        ConfigurationSection schedules = config.getConfigurationSection("scheduled_boosters.schedules");
        if (schedules == null) {
            return;
        }

        for (String key : schedules.getKeys(false)) {
            ConfigurationSection schedule = schedules.getConfigurationSection(key);
            if (schedule == null || !schedule.getBoolean("enabled", false)) {
                continue;
            }

            try {
                BoosterType type = BoosterType.valueOf(schedule.getString("type", "").toUpperCase());
                if (type.isNegativeEffect() && !negativeEffectsEnabled) {
                    continue;
                }

                int hour = schedule.getInt("hour", 0);
                int minute = schedule.getInt("minute", 0);
                int duration = schedule.getInt("duration", 30);
                String activatorName = schedule.getString("activator_name", "Server");

                Set<DayOfWeek> days = new HashSet<>();
                List<String> daysList = schedule.getStringList("days");
                for (String day : daysList) {
                    try {
                        days.add(DayOfWeek.valueOf(day.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid day in schedule " + key + ": " + day);
                    }
                }

                if (!days.isEmpty()) {
                    ScheduledBooster scheduledBooster = new ScheduledBooster(
                            type, hour, minute, duration, activatorName, days
                    );
                    scheduledBoosters.add(scheduledBooster);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load scheduled booster: " + key);
            }
        }
    }

    public double getBoosterPrice(BoosterType type) {
        return boosterPrices.getOrDefault(type, 1000.0);
    }

    public int getBoosterDuration(BoosterType type) {
        return boosterDurations.getOrDefault(type, 30);
    }

    public double getBoosterMultiplier(BoosterType type) {
        return boosterMultipliers.getOrDefault(type, type.getDefaultMultiplier());
    }

    public int getMaxActiveBoosters() {
        return maxActiveBoosters;
    }

    public boolean isLimitedSupplyEnabled() {
        return limitedSupplyEnabled;
    }

    public int getResetHour() {
        return resetHour;
    }

    public boolean isBoosterEnabled(BoosterType type) {
        if (type.isNegativeEffect() && !negativeEffectsEnabled) {
            return false;
        }
        return boosterEnabled.getOrDefault(type, true);
    }

    public int getMaxSupplyPerBooster() {
        return maxSupplyPerBooster;
    }

    public boolean isKeepEffectsOnDeath() {
        return keepEffectsOnDeath;
    }

    public boolean isScheduledBoostersEnabled() {
        return scheduledBoostersEnabled;
    }

    public List<ScheduledBooster> getScheduledBoosters() {
        return new ArrayList<>(scheduledBoosters);
    }

    public String getScheduledBoostersTimezone() {
        return scheduledBoostersTimezone;
    }

    public boolean isShopGuiEnabled() {
        return shopGuiEnabled;
    }

    public boolean isShowActivatorName() {
        return showActivatorName;
    }

    public boolean isNegativeEffectsEnabled() {
        return negativeEffectsEnabled;
    }

    public boolean isRandomScheduledEnabled() {
        return randomScheduledEnabled;
    }

    public int getRandomScheduledInterval() {
        return randomScheduledInterval;
    }

    public int getRandomScheduledDuration() {
        return randomScheduledDuration;
    }

    public String getRandomScheduledActivatorName() {
        return randomScheduledActivatorName;
    }

    public List<String> getRandomScheduledBoosters() {
        return new ArrayList<>(randomScheduledBoosters);
    }

    public double getSoundVolume() {
        return soundVolume;
    }

    private boolean isNoMultiplierBooster(BoosterType type) {
        return switch (type) {
            case NO_FALL_DAMAGE, KEEP_INVENTORY, FLY, PLANT_GROWTH -> true;
            default -> false;
        };
    }

    public void reload() {
        boosterPrices.clear();
        boosterDurations.clear();
        boosterMultipliers.clear();
        boosterEnabled.clear();
        scheduledBoosters.clear();
        randomScheduledBoosters.clear();
        loadConfig();
    }

    private double getDefaultPrice(BoosterType type) {
        return switch (type) {
            case PLANT_GROWTH -> 500.0;
            case SPAWNER_RATE -> 1500.0;
            case EXP_MULTIPLIER -> 1000.0;
            case MOB_DROP -> 1200.0;
            case MINING_SPEED -> 800.0;
            case FISHING_LUCK -> 600.0;
            case FARMING_FORTUNE -> 700.0;
            case COMBAT_DAMAGE -> 2000.0;
            case HASTE, RESISTANCE, JUMP_BOOST, REGENERATION, NIGHT_VISION, FIRE_RESISTANCE, SPEED, STRENGTH -> 1000.0;
            case NO_FALL_DAMAGE -> 800.0;
            case HUNGER_SAVER -> 600.0;
            case ARMOR_DURABILITY -> 1200.0;
            case KEEP_INVENTORY -> 3000.0;
            case FLY -> 2500.0;
            case SLOWNESS, MINING_FATIGUE, WEAKNESS, POISON, BLINDNESS, HUNGER, NAUSEA -> 100.0;
            case WITHER -> 150.0;
            default -> 1000.0;
        };
    }

    public static class ScheduledBooster {
        private final BoosterType type;
        private final int hour;
        private final int minute;
        private final int duration;
        private final String activatorName;
        private final Set<DayOfWeek> days;

        public ScheduledBooster(BoosterType type, int hour, int minute, int duration,
                                String activatorName, Set<DayOfWeek> days) {
            this.type = type;
            this.hour = hour;
            this.minute = minute;
            this.duration = duration;
            this.activatorName = activatorName;
            this.days = days;
        }

        public BoosterType getType() {
            return type;
        }

        public int getHour() {
            return hour;
        }

        public int getMinute() {
            return minute;
        }

        public int getDuration() {
            return duration;
        }

        public String getActivatorName() {
            return activatorName;
        }

        public Set<DayOfWeek> getDays() {
            return days;
        }
    }

    public boolean isCoinsEngineEnabled() {
        return config.getBoolean("economy.coins_engine.enabled", false);
    }

    public String getCoinsEngineCurrency() {
        return config.getString("economy.coins_engine.currency", "money");
    }
}
