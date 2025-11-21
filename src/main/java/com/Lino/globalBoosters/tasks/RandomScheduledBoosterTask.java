package com.Lino.globalBoosters.tasks;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.BoosterType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomScheduledBoosterTask extends BukkitRunnable {

    private final GlobalBoosters plugin;
    private final Random random;
    private long lastActivationTime;

    public RandomScheduledBoosterTask(GlobalBoosters plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.lastActivationTime = 0;
    }

    @Override
    public void run() {
        if (!plugin.getConfigManager().isRandomScheduledEnabled()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        int intervalMinutes = plugin.getConfigManager().getRandomScheduledInterval();
        long intervalMillis = intervalMinutes * 60 * 1000L;

        if (lastActivationTime == 0) {
            lastActivationTime = currentTime - intervalMillis;
        }

        if (currentTime - lastActivationTime < intervalMillis) {
            return;
        }

        List<BoosterType> availableBoosters = getAvailableBoosters();

        if (availableBoosters.isEmpty()) {
            plugin.getLogger().info("Random booster: No boosters available for activation (all active or max limit reached)");
            return;
        }

        BoosterType selectedBooster = availableBoosters.get(random.nextInt(availableBoosters.size()));

        int duration = plugin.getConfigManager().getRandomScheduledDuration();
        String activatorName = plugin.getConfigManager().getRandomScheduledActivatorName();

        boolean activated = plugin.getBoosterManager().activateRandomBooster(
                selectedBooster,
                activatorName,
                duration
        );

        if (activated) {
            lastActivationTime = currentTime;
            plugin.getLogger().info("Random scheduled booster activated: " + selectedBooster.name() +
                    " for " + duration + " minutes");

            announceRandomBoosterActivation(selectedBooster, duration);
        } else {
            plugin.getLogger().info("Random booster: Failed to activate " + selectedBooster.name());
        }
    }

    private List<BoosterType> getAvailableBoosters() {
        List<BoosterType> available = new ArrayList<>();
        List<String> configuredBoosters = plugin.getConfigManager().getRandomScheduledBoosters();

        int maxActive = plugin.getConfigManager().getMaxActiveBoosters();
        int currentActive = plugin.getBoosterManager().getActiveBoosterCount();

        if (maxActive != -1 && currentActive >= maxActive) {
            return available;
        }

        for (String boosterName : configuredBoosters) {
            try {
                BoosterType type = BoosterType.valueOf(boosterName.toUpperCase());

                if (plugin.getConfigManager().isBoosterEnabled(type) &&
                        !plugin.getBoosterManager().isBoosterActive(type)) {
                    available.add(type);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid booster type in random scheduled boosters: " + boosterName);
            }
        }

        return available;
    }

    private void announceRandomBoosterActivation(BoosterType type, int duration) {
        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
        placeholders.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(type));
        placeholders.put("%duration%", String.valueOf(duration));

        String message = plugin.getMessagesManager().getMessage("booster.activated-random", placeholders);
        plugin.getMessagesManager().sendBroadCastMessage(message);
    }
}
