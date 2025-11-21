package com.Lino.globalBoosters.listeners;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.ActiveBooster;
import com.Lino.globalBoosters.boosters.BoosterType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FlyBoosterListener implements Listener {

    private final GlobalBoosters plugin;
    private final Set<UUID> originallyFlying;
    private final Set<UUID> boosterFlyingPlayers;

    public FlyBoosterListener(GlobalBoosters plugin) {
        this.plugin = plugin;
        this.originallyFlying = new HashSet<>();
        this.boosterFlyingPlayers = new HashSet<>();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (boosterFlyingPlayers.contains(playerId) && !player.hasPermission("globalboosters.fly.bypass")) {
            player.setAllowFlight(false);
            player.setFlying(false);
            boosterFlyingPlayers.remove(playerId);
        }

        if (plugin.getBoosterManager().isBoosterActive(BoosterType.FLY)) {
            ActiveBooster booster = plugin.getBoosterManager().getActiveBooster(BoosterType.FLY);
            if (booster != null && !booster.isExpired()) {
                if (!player.getAllowFlight() && !player.hasPermission("globalboosters.fly.bypass")) {
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    boosterFlyingPlayers.add(playerId);
                }
            } else {
                if (boosterFlyingPlayers.contains(playerId) && !player.hasPermission("globalboosters.fly.bypass")) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    boosterFlyingPlayers.remove(playerId);
                }
            }
        } else {
            if (boosterFlyingPlayers.contains(playerId) && !player.hasPermission("globalboosters.fly.bypass")) {
                player.setAllowFlight(false);
                player.setFlying(false);
                boosterFlyingPlayers.remove(playerId);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (player.getAllowFlight() && plugin.getBoosterManager().isBoosterActive(BoosterType.FLY)) {
            if (!originallyFlying.contains(playerId) && !player.hasPermission("globalboosters.fly.bypass")) {
                boosterFlyingPlayers.add(playerId);
            }
        }
    }

    public void enableFlyForAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            if (player.getAllowFlight()) {
                originallyFlying.add(playerId);
            } else {
                player.setAllowFlight(true);
                player.setFlying(true);
                boosterFlyingPlayers.add(playerId);
            }
        }
    }

    public void disableFlyForAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            if (!originallyFlying.contains(playerId) && !player.hasPermission("globalboosters.fly.bypass")) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }
        originallyFlying.clear();
        boosterFlyingPlayers.clear();
    }

    public void cleanupOfflinePlayerFly() {
        boosterFlyingPlayers.clear();
        originallyFlying.clear();
    }
}
