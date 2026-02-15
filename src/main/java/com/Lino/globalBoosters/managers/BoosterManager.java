package com.Lino.globalBoosters.managers;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.ActiveBooster;
import com.Lino.globalBoosters.boosters.BoosterType;
import com.Lino.globalBoosters.boosters.QueuedBooster;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BoosterManager {

    private final GlobalBoosters plugin;
    private final Map<BoosterType, ActiveBooster> activeBoosters;
    private final Map<BoosterType, Queue<QueuedBooster>> boosterQueues;
    private final UUID serverUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public BoosterManager(GlobalBoosters plugin) {
        this.plugin = plugin;
        this.activeBoosters = new ConcurrentHashMap<>();
        this.boosterQueues = new ConcurrentHashMap<>();
    }

    public boolean activateBooster(BoosterType type, Player activator, int durationMinutes) {
        if (isBoosterActive(type)) {
            if (plugin.getConfigManager().isQueueEnabled()) {
                return queueBooster(type, activator.getUniqueId(), activator.getName(), durationMinutes, "Player Item");
            }
            activator.sendMessage(plugin.getMessagesManager().getMessage("booster.already-active"));
            return false;
        }

        int maxActive = plugin.getConfigManager().getMaxActiveBoosters();
        if (maxActive != -1 && activeBoosters.size() >= maxActive) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%max%", String.valueOf(maxActive));
            activator.sendMessage(plugin.getMessagesManager().getMessage("booster.max-active-reached", placeholders));
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

        plugin.getBoosterLogger().logActivation(type, activator.getName(), durationMinutes, "Player Item");

        return true;
    }

    public boolean activateScheduledBooster(BoosterType type, String activatorName, int durationMinutes) {
        if (isBoosterActive(type)) {
            if (plugin.getConfigManager().isQueueEnabled()) {
                return queueBooster(type, serverUUID, activatorName, durationMinutes, "Scheduled");
            }
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

        plugin.getBoosterLogger().logActivation(type, activatorName, durationMinutes, "Scheduled");

        return true;
    }

    public boolean activateRandomBooster(BoosterType type, String activatorName, int durationMinutes) {
        if (isBoosterActive(type)) {
            if (plugin.getConfigManager().isQueueEnabled()) {
                return queueBooster(type, serverUUID, activatorName, durationMinutes, "Random");
            }
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

        plugin.getBoosterLogger().logActivation(type, activatorName, durationMinutes, "Random");

        return true;
    }

    public boolean activateConsoleBooster(BoosterType type, String activatorName, int durationMinutes) {
        if (isBoosterActive(type)) {
            if (plugin.getConfigManager().isQueueEnabled()) {
                return queueBooster(type, UUID.randomUUID(), activatorName, durationMinutes, "Console");
            }
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

        plugin.getBoosterLogger().logActivation(type, activatorName, durationMinutes, "Console");

        return true;
    }

    private boolean queueBooster(BoosterType type, UUID activatorUUID, String activatorName, int durationMinutes, String source) {
        Queue<QueuedBooster> queue = boosterQueues.computeIfAbsent(type, k -> new ConcurrentLinkedQueue<>());
        QueuedBooster queued = new QueuedBooster(type, activatorUUID, activatorName, durationMinutes, source);
        queue.add(queued);

        int position = queue.size();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(type));
        placeholders.put("%position%", String.valueOf(position));

        Player player = Bukkit.getPlayer(activatorUUID);
        if (player != null && player.isOnline()) {
            player.sendMessage(plugin.getMessagesManager().getMessage("booster.queued", placeholders));
        }

        plugin.getBoosterLogger().logQueue(type, activatorName, durationMinutes);
        return true;
    }

    private void activateFromQueue(BoosterType type) {
        Queue<QueuedBooster> queue = boosterQueues.get(type);
        if (queue == null || queue.isEmpty()) {
            return;
        }

        QueuedBooster next = queue.poll();

        int maxActive = plugin.getConfigManager().getMaxActiveBoosters();
        if (maxActive != -1 && activeBoosters.size() >= maxActive) {
            queue.add(next);
            return;
        }

        ActiveBooster booster = new ActiveBooster(next.getType(), next.getActivatorUUID(), next.getActivatorName(), next.getDurationMinutes());
        activeBoosters.put(type, booster);

        plugin.getBossBarManager().createBossBar(booster);
        plugin.getDataManager().saveActiveBooster(booster);
        plugin.getDataManager().incrementBoosterUsage(type);

        if (type.isEffectBooster()) {
            plugin.getEffectBoosterListener().applyEffectToAll(type);
        } else if (type == BoosterType.FLY) {
            plugin.getFlyBoosterListener().enableFlyForAll();
        }

        announceBoosterActivation(type, next.getActivatorName(), next.getDurationMinutes(), false);
        playGlobalSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);

        plugin.getBoosterLogger().logActivation(type, next.getActivatorName(), next.getDurationMinutes(), "Queue (" + next.getSource() + ")");

        int remaining = queue.size();
        if (remaining > 0) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(type));
            placeholders.put("%count%", String.valueOf(remaining));
            String msg = plugin.getMessagesManager().getMessage("booster.queue-remaining", placeholders);
            Bukkit.broadcastMessage(msg);
        }
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

            plugin.getBoosterLogger().logDeactivation(type, "Expired");

            if (plugin.getConfigManager().isQueueEnabled()) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    activateFromQueue(type);
                }, 20L);
            }

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

    public int getQueueSize(BoosterType type) {
        Queue<QueuedBooster> queue = boosterQueues.get(type);
        return queue == null ? 0 : queue.size();
    }

    public int getTotalQueueSize() {
        int total = 0;
        for (Queue<QueuedBooster> queue : boosterQueues.values()) {
            total += queue.size();
        }
        return total;
    }

    public void clearQueue(BoosterType type) {
        Queue<QueuedBooster> queue = boosterQueues.get(type);
        if (queue != null) {
            queue.clear();
        }
    }

    public void clearAllQueues() {
        boosterQueues.clear();
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
        Bukkit.broadcastMessage(message);
    }

    private void announceBoosterDeactivation(BoosterType type) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%prefix%", plugin.getMessagesManager().getPrefix());
        placeholders.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(type));

        String message = plugin.getMessagesManager().getMessage("booster.expired", placeholders);
        Bukkit.broadcastMessage(message);
    }

    private void playGlobalSound(Sound sound, float volume, float pitch) {
        float finalVolume = (float) (volume * plugin.getConfigManager().getSoundVolume());
        if (finalVolume <= 0) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, finalVolume, pitch);
        }
    }
}