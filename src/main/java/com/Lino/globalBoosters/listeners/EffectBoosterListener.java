package com.Lino.globalBoosters.listeners;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.ActiveBooster;
import com.Lino.globalBoosters.boosters.BoosterType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EffectBoosterListener implements Listener {

    private final GlobalBoosters plugin;
    private final Map<BoosterType, PotionEffectType> effectMap;
    private final Set<UUID> playersWithBoosterEffects;

    public EffectBoosterListener(GlobalBoosters plugin) {
        this.plugin = plugin;
        this.effectMap = new HashMap<>();
        this.playersWithBoosterEffects = new HashSet<>();
        initializeEffectMap();
    }

    private void initializeEffectMap() {
        effectMap.put(BoosterType.HASTE, PotionEffectType.HASTE);
        effectMap.put(BoosterType.RESISTANCE, PotionEffectType.RESISTANCE);
        effectMap.put(BoosterType.JUMP_BOOST, PotionEffectType.JUMP_BOOST);
        effectMap.put(BoosterType.REGENERATION, PotionEffectType.REGENERATION);
        effectMap.put(BoosterType.NIGHT_VISION, PotionEffectType.NIGHT_VISION);
        effectMap.put(BoosterType.FIRE_RESISTANCE, PotionEffectType.FIRE_RESISTANCE);
        effectMap.put(BoosterType.SPEED, PotionEffectType.SPEED);
        effectMap.put(BoosterType.STRENGTH, PotionEffectType.STRENGTH);
        effectMap.put(BoosterType.SLOWNESS, PotionEffectType.SLOWNESS);
        effectMap.put(BoosterType.MINING_FATIGUE, PotionEffectType.MINING_FATIGUE);
        effectMap.put(BoosterType.WEAKNESS, PotionEffectType.WEAKNESS);
        effectMap.put(BoosterType.POISON, PotionEffectType.POISON);
        effectMap.put(BoosterType.WITHER, PotionEffectType.WITHER);
        effectMap.put(BoosterType.BLINDNESS, PotionEffectType.BLINDNESS);
        effectMap.put(BoosterType.HUNGER, PotionEffectType.HUNGER);
        effectMap.put(BoosterType.NAUSEA, PotionEffectType.NAUSEA);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        removeAllBoosterEffects(player);

        boolean hasActiveBooster = false;
        for (Map.Entry<BoosterType, PotionEffectType> entry : effectMap.entrySet()) {
            BoosterType boosterType = entry.getKey();
            PotionEffectType effectType = entry.getValue();

            if (plugin.getBoosterManager().isBoosterActive(boosterType)) {
                ActiveBooster booster = plugin.getBoosterManager().getActiveBooster(boosterType);
                if (booster != null && !booster.isExpired()) {
                    int amplifier = (int) (plugin.getConfigManager().getBoosterMultiplier(boosterType) - 1);
                    int duration = (int) (booster.getRemainingSeconds() * 20);

                    if (duration > 0) {
                        duration = Math.min(duration, 72000);
                        player.addPotionEffect(new PotionEffect(effectType, duration, amplifier, false, false));
                        hasActiveBooster = true;
                    }
                }
            }
        }

        if (hasActiveBooster) {
            playersWithBoosterEffects.add(playerId);
        } else {
            playersWithBoosterEffects.remove(playerId);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        boolean hasBoosterEffect = false;
        for (Map.Entry<BoosterType, PotionEffectType> entry : effectMap.entrySet()) {
            if (player.hasPotionEffect(entry.getValue()) && plugin.getBoosterManager().isBoosterActive(entry.getKey())) {
                hasBoosterEffect = true;
                break;
            }
        }

        if (hasBoosterEffect) {
            playersWithBoosterEffects.add(playerId);
        } else {
            playersWithBoosterEffects.remove(playerId);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (plugin.getConfigManager().isKeepEffectsOnDeath()) {
            Player player = event.getPlayer();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                applyActiveEffects(player);
                if (plugin.getBoosterManager().isBoosterActive(BoosterType.FLY)) {
                    ActiveBooster booster = plugin.getBoosterManager().getActiveBooster(BoosterType.FLY);
                    if (booster != null && !booster.isExpired()) {
                        if (!player.getAllowFlight()) {
                            player.setAllowFlight(true);
                            player.setFlying(true);
                        }
                    }
                }
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfigManager().isKeepEffectsOnDeath()) {
            Player player = event.getEntity();
            removeAllBoosterEffects(player);
        }
    }

    public void applyActiveEffects(Player player) {
        UUID playerId = player.getUniqueId();

        removeAllBoosterEffects(player);

        boolean hasActiveBooster = false;
        for (Map.Entry<BoosterType, PotionEffectType> entry : effectMap.entrySet()) {
            BoosterType boosterType = entry.getKey();
            PotionEffectType effectType = entry.getValue();

            if (plugin.getBoosterManager().isBoosterActive(boosterType)) {
                ActiveBooster booster = plugin.getBoosterManager().getActiveBooster(boosterType);
                if (booster != null && !booster.isExpired()) {
                    int amplifier = (int) (plugin.getConfigManager().getBoosterMultiplier(boosterType) - 1);
                    int duration = (int) (booster.getRemainingSeconds() * 20);

                    if (duration > 0) {
                        duration = Math.min(duration, 72000);
                        player.addPotionEffect(new PotionEffect(effectType, duration, amplifier, false, false));
                        hasActiveBooster = true;
                    }
                }
            }
        }

        if (hasActiveBooster) {
            playersWithBoosterEffects.add(playerId);
        } else {
            playersWithBoosterEffects.remove(playerId);
        }
    }

    public void applyEffectToAll(BoosterType type) {
        PotionEffectType effectType = effectMap.get(type);
        if (effectType != null) {
            ActiveBooster booster = plugin.getBoosterManager().getActiveBooster(type);
            if (booster != null && !booster.isExpired()) {
                int amplifier = (int) (plugin.getConfigManager().getBoosterMultiplier(type) - 1);
                int duration = (int) (booster.getRemainingSeconds() * 20);

                if (duration > 0) {
                    duration = Math.min(duration, 72000);

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.addPotionEffect(new PotionEffect(effectType, duration, amplifier, false, false));
                        playersWithBoosterEffects.add(player.getUniqueId());
                    }
                }
            }
        }
    }

    public void removeEffectFromAll(BoosterType type) {
        PotionEffectType effectType = effectMap.get(type);
        if (effectType != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.removePotionEffect(effectType);
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean hasOtherEffects = false;
            for (Map.Entry<BoosterType, PotionEffectType> entry : effectMap.entrySet()) {
                if (entry.getKey() != type && plugin.getBoosterManager().isBoosterActive(entry.getKey())) {
                    if (player.hasPotionEffect(entry.getValue())) {
                        hasOtherEffects = true;
                        break;
                    }
                }
            }

            if (!hasOtherEffects) {
                playersWithBoosterEffects.remove(player.getUniqueId());
            }
        }
    }

    public void updateEffectDuration(BoosterType type) {
        PotionEffectType effectType = effectMap.get(type);
        if (effectType != null) {
            ActiveBooster booster = plugin.getBoosterManager().getActiveBooster(type);
            if (booster != null && !booster.isExpired()) {
                int amplifier = (int) (plugin.getConfigManager().getBoosterMultiplier(type) - 1);
                int duration = (int) (booster.getRemainingSeconds() * 20);

                if (duration > 0) {
                    duration = Math.min(duration, 72000);

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (playersWithBoosterEffects.contains(player.getUniqueId())) {
                            player.removePotionEffect(effectType);
                            player.addPotionEffect(new PotionEffect(effectType, duration, amplifier, false, false));
                        }
                    }
                }
            } else {
                removeEffectFromAll(type);
            }
        }
    }

    private void removeAllBoosterEffects(Player player) {
        for (PotionEffectType effectType : effectMap.values()) {
            player.removePotionEffect(effectType);
        }
    }

    public void cleanupOfflinePlayerEffects() {
        playersWithBoosterEffects.clear();
    }
}
