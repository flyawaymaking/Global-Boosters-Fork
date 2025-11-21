package com.Lino.globalBoosters;

import com.Lino.globalBoosters.commands.BoostShopCommand;
import com.Lino.globalBoosters.commands.BoosterCommand;
import com.Lino.globalBoosters.commands.GlobalBoostersCommand;
import com.Lino.globalBoosters.config.ConfigManager;
import com.Lino.globalBoosters.config.MessagesManager;
import com.Lino.globalBoosters.data.DataManager;
import com.Lino.globalBoosters.listeners.BoosterItemListener;
import com.Lino.globalBoosters.listeners.EffectBoosterListener;
import com.Lino.globalBoosters.listeners.FlyBoosterListener;
import com.Lino.globalBoosters.listeners.GameEventListener;
import com.Lino.globalBoosters.managers.BoosterManager;
import com.Lino.globalBoosters.managers.BossBarManager;
import com.Lino.globalBoosters.managers.EconomyManager;
import com.Lino.globalBoosters.managers.SupplyManager;
import com.Lino.globalBoosters.tasks.BoosterTickTask;
import com.Lino.globalBoosters.tasks.ScheduledBoosterTask;
import com.Lino.globalBoosters.tasks.RandomScheduledBoosterTask;
import com.Lino.globalBoosters.boosters.BoosterType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.potion.PotionEffectType;

public class GlobalBoosters extends JavaPlugin {

    private static GlobalBoosters instance;
    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private DataManager dataManager;
    private BoosterManager boosterManager;
    private BossBarManager bossBarManager;
    private BoosterItemListener boosterItemListener;
    private EffectBoosterListener effectBoosterListener;
    private FlyBoosterListener flyBoosterListener;
    private SupplyManager supplyManager;
    private BukkitTask scheduledBoosterTask;
    private BukkitTask randomScheduledBoosterTask;
    private EconomyManager economyManager;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        initializeManagers();

        if (configManager.isShopGuiEnabled()) {
            if (economyManager.getProvider() == null) {
                getLogger().severe("No economy provider found! Disabling plugin...");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        registerCommands();
        registerListeners();
        startTasks();

        getLogger().info("GlobalBoosters has been enabled!");
    }

    private void initializeManagers() {
        configManager = new ConfigManager(this);
        messagesManager = new MessagesManager(this);
        dataManager = new DataManager(this);
        economyManager = new EconomyManager(this); // Новый менеджер
        boosterManager = new BoosterManager(this);
        bossBarManager = new BossBarManager(this);
        supplyManager = new SupplyManager(this);

        dataManager.loadActiveBoosters();
    }

    @Override
    public void onDisable() {
        if (scheduledBoosterTask != null) {
            scheduledBoosterTask.cancel();
        }

        if (randomScheduledBoosterTask != null) {
            randomScheduledBoosterTask.cancel();
        }

        cleanupAllEffects();

        if (boosterManager != null) {
            boosterManager.saveAllBoosters();
        }
        if (bossBarManager != null) {
            bossBarManager.removeAllBossBars();
        }
        if (supplyManager != null) {
            supplyManager.shutdown();
        }
        if (dataManager != null) {
            dataManager.closeConnection();
        }

        getLogger().info("GlobalBoosters has been disabled!");
    }

    private void cleanupAllEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removePotionEffect(PotionEffectType.HASTE);
            player.removePotionEffect(PotionEffectType.RESISTANCE);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            player.removePotionEffect(PotionEffectType.REGENERATION);
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.STRENGTH);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            player.removePotionEffect(PotionEffectType.WEAKNESS);
            player.removePotionEffect(PotionEffectType.POISON);
            player.removePotionEffect(PotionEffectType.WITHER);
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.HUNGER);
            player.removePotionEffect(PotionEffectType.NAUSEA);

            if (flyBoosterListener != null) {
                flyBoosterListener.cleanupOfflinePlayerFly();
            }

            if (boosterManager != null) {
                for (BoosterType type : BoosterType.values()) {
                    if (type == BoosterType.FLY && boosterManager.isBoosterActive(type)) {
                        if (!player.hasPermission("globalboosters.fly.bypass")) {
                            player.setAllowFlight(false);
                            player.setFlying(false);
                        }
                    }
                }
            }
        }

        if (effectBoosterListener != null) {
            effectBoosterListener.cleanupOfflinePlayerEffects();
        }
    }

    private void registerCommands() {
        BoostShopCommand boostShopCommand = new BoostShopCommand(this);
        getCommand("boostshop").setExecutor(boostShopCommand);
        getCommand("boostshop").setTabCompleter(boostShopCommand);

        GlobalBoostersCommand globalBoostersCommand = new GlobalBoostersCommand(this);
        getCommand("globalboosters").setExecutor(globalBoostersCommand);
        getCommand("globalboosters").setTabCompleter(globalBoostersCommand);

        BoosterCommand boosterCommand = new BoosterCommand(this);
        getCommand("booster").setExecutor(boosterCommand);
        getCommand("booster").setTabCompleter(boosterCommand);
    }

    private void registerListeners() {
        boosterItemListener = new BoosterItemListener(this);
        effectBoosterListener = new EffectBoosterListener(this);
        flyBoosterListener = new FlyBoosterListener(this);

        getServer().getPluginManager().registerEvents(boosterItemListener, this);
        getServer().getPluginManager().registerEvents(effectBoosterListener, this);
        getServer().getPluginManager().registerEvents(flyBoosterListener, this);
        getServer().getPluginManager().registerEvents(new GameEventListener(this), this);
    }

    private void startTasks() {
        new BoosterTickTask(this).runTaskTimer(this, 20L, 20L);

        if (configManager.isScheduledBoostersEnabled()) {
            scheduledBoosterTask = new ScheduledBoosterTask(this).runTaskTimer(this, 100L, 1200L);
        }

        if (configManager.isRandomScheduledEnabled()) {
            randomScheduledBoosterTask = new RandomScheduledBoosterTask(this).runTaskTimer(this, 20L, 20L);
        }
    }

    public void reloadScheduledTask() {
        if (scheduledBoosterTask != null) {
            scheduledBoosterTask.cancel();
            scheduledBoosterTask = null;
        }

        if (randomScheduledBoosterTask != null) {
            randomScheduledBoosterTask.cancel();
            randomScheduledBoosterTask = null;
        }

        if (configManager.isScheduledBoostersEnabled()) {
            scheduledBoosterTask = new ScheduledBoosterTask(this).runTaskTimer(this, 100L, 1200L);
        }

        if (configManager.isRandomScheduledEnabled()) {
            randomScheduledBoosterTask = new RandomScheduledBoosterTask(this).runTaskTimer(this, 100L, 1200L);
        }
    }

    public static GlobalBoosters getInstance() {
        return instance;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public BoosterManager getBoosterManager() {
        return boosterManager;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public BoosterItemListener getBoosterItemListener() {
        return boosterItemListener;
    }

    public EffectBoosterListener getEffectBoosterListener() {
        return effectBoosterListener;
    }

    public FlyBoosterListener getFlyBoosterListener() {
        return flyBoosterListener;
    }

    public SupplyManager getSupplyManager() {
        return supplyManager;
    }
}
