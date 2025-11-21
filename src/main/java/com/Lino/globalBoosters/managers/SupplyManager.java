package com.Lino.globalBoosters.managers;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.BoosterType;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SupplyManager {

    private final GlobalBoosters plugin;
    private final Map<BoosterType, Integer> purchaseCounts;
    private final Map<BoosterType, Long> lastResetTime;
    private Connection connection;
    private final int maxPurchases;

    public SupplyManager(GlobalBoosters plugin) {
        this.plugin = plugin;
        this.purchaseCounts = new ConcurrentHashMap<>();
        this.lastResetTime = new ConcurrentHashMap<>();
        this.maxPurchases = plugin.getConfigManager().getMaxSupplyPerBooster();

        initializeDatabase();
        loadData();
        startResetTask();
    }

    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/supply_data.db");

            String createTable = "CREATE TABLE IF NOT EXISTS supply_data (" +
                    "booster_type VARCHAR(50) PRIMARY KEY," +
                    "purchase_count INTEGER DEFAULT 0," +
                    "last_reset BIGINT DEFAULT 0)";
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createTable);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Could not initialize supply database!");
            e.printStackTrace();
        }
    }

    private void loadData() {
        String query = "SELECT * FROM supply_data";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                try {
                    BoosterType type = BoosterType.valueOf(rs.getString("booster_type"));
                    purchaseCounts.put(type, rs.getInt("purchase_count"));
                    lastResetTime.put(type, rs.getLong("last_reset"));
                } catch (IllegalArgumentException e) {
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for (BoosterType type : BoosterType.values()) {
            if (!purchaseCounts.containsKey(type)) {
                purchaseCounts.put(type, 0);
                lastResetTime.put(type, System.currentTimeMillis());
                saveBoosterData(type);
            }
            checkAndResetIfNeeded(type);
        }
    }

    private void saveBoosterData(BoosterType type) {
        String query = "INSERT OR REPLACE INTO supply_data (booster_type, purchase_count, last_reset) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, type.name());
            pstmt.setInt(2, purchaseCounts.get(type));
            pstmt.setLong(3, lastResetTime.get(type));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean canPurchase(BoosterType type) {
        if (!plugin.getConfigManager().isLimitedSupplyEnabled()) {
            return true;
        }

        checkAndResetIfNeeded(type);
        int currentCount = purchaseCounts.get(type);
        plugin.getLogger().info("Supply check for " + type.name() + ": " + currentCount + "/" + maxPurchases);
        return currentCount < maxPurchases;
    }

    public synchronized int getRemainingPurchases(BoosterType type) {
        if (!plugin.getConfigManager().isLimitedSupplyEnabled()) {
            return -1;
        }

        checkAndResetIfNeeded(type);
        int remaining = maxPurchases - purchaseCounts.get(type);
        return Math.max(0, remaining);
    }

    public synchronized boolean recordPurchase(BoosterType type) {
        if (!plugin.getConfigManager().isLimitedSupplyEnabled()) {
            return true;
        }

        checkAndResetIfNeeded(type);

        int currentCount = purchaseCounts.get(type);
        if (currentCount >= maxPurchases) {
            plugin.getLogger().warning("Attempted to purchase " + type.name() + " but supply is exhausted!");
            return false;
        }

        purchaseCounts.put(type, currentCount + 1);
        saveBoosterData(type);
        plugin.getLogger().info("Purchase recorded for " + type.name() + ": " + (currentCount + 1) + "/" + maxPurchases);
        return true;
    }

    public String getTimeUntilReset(BoosterType type) {
        if (!plugin.getConfigManager().isLimitedSupplyEnabled()) {
            return "";
        }

        long nextReset = getNextResetTime();
        long timeRemaining = nextReset - System.currentTimeMillis();
        if (timeRemaining <= 0) {
            return "Resetting soon...";
        }

        long hours = timeRemaining / (1000 * 60 * 60);
        long minutes = (timeRemaining % (1000 * 60 * 60)) / (1000 * 60);
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    private void checkAndResetIfNeeded(BoosterType type) {
        long lastReset = lastResetTime.get(type);
        long nextReset = getNextResetTime();
        long currentTime = System.currentTimeMillis();

        if (currentTime >= nextReset && lastReset < getPreviousResetTime()) {
            purchaseCounts.put(type, 0);
            lastResetTime.put(type, currentTime);
            saveBoosterData(type);
        }
    }

    private void resetAllSupplies() {
        boolean wasReset = false;
        for (BoosterType type : BoosterType.values()) {
            if (purchaseCounts.get(type) > 0) {
                wasReset = true;
            }
            purchaseCounts.put(type, 0);
            lastResetTime.put(type, System.currentTimeMillis());
            saveBoosterData(type);
        }

        if (wasReset) {
            announceRestock();
        }
    }

    private void announceRestock() {
        String message = plugin.getMessagesManager().getMessage("supply.restocked");
        plugin.getMessagesManager().sendBroadCastMessage(message);

        float volume = (float) plugin.getConfigManager().getSoundVolume();
        if (volume <= 0) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, volume, 1.5f);
        }
    }

    private void startResetTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                long nextReset = getNextResetTime();

                if (currentTime >= nextReset) {
                    resetAllSupplies();
                }
            }
        }.runTaskTimer(plugin, 200L, 200L);
    }

    private long getNextResetTime() {
        int resetHour = plugin.getConfigManager().getResetHour();
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime nextReset = now.withHour(resetHour).withMinute(0).withSecond(0).withNano(0);

        if (now.isAfter(nextReset)) {
            nextReset = nextReset.plusDays(1);
        }

        return nextReset.toInstant().toEpochMilli();
    }

    private long getPreviousResetTime() {
        int resetHour = plugin.getConfigManager().getResetHour();
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime previousReset = now.withHour(resetHour).withMinute(0).withSecond(0).withNano(0);

        if (now.isBefore(previousReset)) {
            previousReset = previousReset.minusDays(1);
        }

        return previousReset.toInstant().toEpochMilli();
    }

    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
