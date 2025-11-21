package com.Lino.globalBoosters.managers;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.ActiveBooster;
import com.Lino.globalBoosters.boosters.BoosterType;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BoosterManager {

    private final GlobalBoosters plugin;
    private final Map<BoosterType, ActiveBooster> activeBoosters;
    private final UUID serverUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public BoosterManager(GlobalBoosters plugin) {
        this.plugin = plugin;
        this.activeBoosters = new ConcurrentHashMap<>();
    }

    public boolean activateBooster(BoosterType type, Player activator, int durationMinutes) {
        if (isBoosterActive(type)) {
            plugin.getMessagesManager().sendMessage(activator, plugin.getMessagesManager().getMessage("booster.already-active"));
            return false;
        }

        int maxActive = plugin.getConfigManager().getMaxActiveBoosters();
        if (maxActive != -1 && activeBoosters.size() >= maxActive) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%max%", String.valueOf(maxActive));
            plugin.getMessagesManager().sendMessage(activator, plugin.getMessagesManager().getMessage("booster.max-active-reached", placeholders));
            return false;
        }

        ActiveBooster booster = new ActiveBooster(type, activator.getUniqueId(), activator.getName(), durationMinutes);
        activeBoosters.put(type, booster);

        plugin.getBossBarManager().createBossBar(booster);
        plugin.getDataManager().saveActiveBooster(booster);
        plugin.getDataManager().incrementBoosterUsage(type);

        if (type.isEffectBooster()) {
            plugin.getEffectBoosterListener().applyEffectToAll(type);
        } else if (type == BoosterType.FLY) {
            plugin.getFlyBoosterListener().enableFlyForAll();
        }

        announceBoosterActivation(type, activator.getName(), durationMinutes, false);
        playGlobalSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);

        return true;
    }

    public boolean activateScheduledBooster(BoosterType type, String activatorName, int durationMinutes) {
        if (isBoosterActive(type)) {
            return false;
        }

        int maxActive = plugin.getConfigManager().getMaxActiveBoosters();
        if (maxActive != -1 && activeBoosters.size() >= maxActive) {
            return false;
        }

        ActiveBooster booster = new ActiveBooster(type, serverUUID, activatorName, durationMinutes);
        activeBoosters.put(type, booster);

        plugin.getBossBarManager().createBossBar(booster);
        plugin.getDataManager().saveActiveBooster(booster);
        plugin.getDataManager().incrementBoosterUsage(type);

        if (type.isEffectBooster()) {
            plugin.getEffectBoosterListener().applyEffectToAll(type);
        } else if (type == BoosterType.FLY) {
            plugin.getFlyBoosterListener().enableFlyForAll();
        }

        announceBoosterActivation(type, activatorName, durationMinutes, true);
        playGlobalSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);

        return true;
    }

    public boolean activateRandomBooster(BoosterType type, String activatorName, int durationMinutes) {
        if (isBoosterActive(type)) {
            return false;
        }

        int maxActive = plugin.getConfigManager().getMaxActiveBoosters();
        if (maxActive != -1 && activeBoosters.size() >= maxActive) {
            return false;
        }

        ActiveBooster booster = new ActiveBooster(type, serverUUID, activatorName, durationMinutes);
        activeBoosters.put(type, booster);

        plugin.getBossBarManager().createBossBar(booster);
        plugin.getDataManager().saveActiveBooster(booster);
        plugin.getDataManager().incrementBoosterUsage(type);

        if (type.isEffectBooster()) {
            plugin.getEffectBoosterListener().applyEffectToAll(type);
        } else if (type == BoosterType.FLY) {
            plugin.getFlyBoosterListener().enableFlyForAll();
        }

        playGlobalSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);

        return true;
    }

    public boolean activateConsoleBooster(BoosterType type, String activatorName, int durationMinutes) {
        if (isBoosterActive(type)) {
            return false;
        }

        int maxActive = plugin.getConfigManager().getMaxActiveBoosters();
        if (maxActive != -1 && activeBoosters.size() >= maxActive) {
            return false;
        }

        ActiveBooster booster = new ActiveBooster(type, UUID.randomUUID(), activatorName, durationMinutes);
        activeBoosters.put(type, booster);

        plugin.getBossBarManager().createBossBar(booster);
        plugin.getDataManager().saveActiveBooster(booster);
        plugin.getDataManager().incrementBoosterUsage(type);

        if (type.isEffectBooster()) {
            plugin.getEffectBoosterListener().applyEffectToAll(type);
        } else if (type == BoosterType.FLY) {
            plugin.getFlyBoosterListener().enableFlyForAll();
        }

        announceBoosterActivation(type, activatorName, durationMinutes, false);
        playGlobalSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);

        return true;
    }

    public boolean deactivateBooster(BoosterType type) {
        ActiveBooster booster = activeBoosters.remove(type);
        if (booster != null) {
            plugin.getBossBarManager().removeBossBar(type);
            plugin.getDataManager().removeActiveBooster(type);
            if (type.isEffectBooster()) {
                plugin.getEffectBoosterListener().removeEffectFromAll(type);
            } else if (type == BoosterType.FLY) {
                plugin.getFlyBoosterListener().disableFlyForAll();
            }

            announceBoosterDeactivation(type);
            playGlobalSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);
            return true;
        }
        return false;
    }

    public void tickAllBoosters() {
        List<BoosterType> toRemove = new ArrayList<>();
        for (Map.Entry<BoosterType, ActiveBooster> entry : activeBoosters.entrySet()) {
            ActiveBooster booster = entry.getValue();
            booster.tick();
            plugin.getBossBarManager().updateBossBar(booster);

            if (booster.isExpired()) {
                toRemove.add(entry.getKey());
            }
        }

        for (BoosterType type : toRemove) {
            deactivateBooster(type);
        }
    }

    public void saveAllBoosters() {
        for (ActiveBooster booster : activeBoosters.values()) {
            plugin.getDataManager().saveActiveBooster(booster);
        }
    }

    public void loadBooster(ActiveBooster booster) {
        if (booster.isExpired()) {
            plugin.getDataManager().removeActiveBooster(booster.getType());
            return;
        }

        activeBoosters.put(booster.getType(), booster);
        plugin.getBossBarManager().createBossBar(booster);
        if (booster.getType().isEffectBooster()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getEffectBoosterListener().applyEffectToAll(booster.getType());
            }, 20L);
        } else if (booster.getType() == BoosterType.FLY) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getFlyBoosterListener().enableFlyForAll();
            }, 20L);
        }
    }

    public boolean isBoosterActive(BoosterType type) {
        ActiveBooster booster = activeBoosters.get(type);
        if (booster != null && booster.isExpired()) {
            deactivateBooster(type);
            return false;
        }
        return booster != null;
    }

    public ActiveBooster getActiveBooster(BoosterType type) {
        ActiveBooster booster = activeBoosters.get(type);
        if (booster != null && booster.isExpired()) {
            deactivateBooster(type);
            return null;
        }
        return booster;
    }

    public Collection<ActiveBooster> getActiveBoosters() {
        List<ActiveBooster> active = new ArrayList<>();
        for (ActiveBooster booster : activeBoosters.values()) {
            if (!booster.isExpired()) {
                active.add(booster);
            }
        }
        return active;
    }

    public int getActiveBoosterCount() {
        return getActiveBoosters().size();
    }

    public double getMultiplier(BoosterType type) {
        if (isBoosterActive(type)) {
            return plugin.getConfigManager().getBoosterMultiplier(type);
        }
        return 1.0;
    }

    private void announceBoosterActivation(BoosterType type, String activatorName, int durationMinutes, boolean scheduled) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%prefix%", plugin.getMessagesManager().getPrefix());
        placeholders.put("%player%", activatorName);
        placeholders.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(type));
        placeholders.put("%duration%", String.valueOf(durationMinutes));

        String messageKey = scheduled ? "booster.activated-scheduled" : "booster.activated";
        String message = plugin.getMessagesManager().getMessage(messageKey, placeholders);
        plugin.getMessagesManager().sendBroadCastMessage(message);
    }

    private void announceBoosterDeactivation(BoosterType type) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%prefix%", plugin.getMessagesManager().getPrefix());
        placeholders.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(type));

        String message = plugin.getMessagesManager().getMessage("booster.expired", placeholders);
        plugin.getMessagesManager().sendBroadCastMessage(message);
    }

    private void playGlobalSound(Sound sound, float volume, float pitch) {
        float finalVolume = (float) (volume * plugin.getConfigManager().getSoundVolume());
        if (finalVolume <= 0) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, finalVolume, pitch);
        }
    }
}
