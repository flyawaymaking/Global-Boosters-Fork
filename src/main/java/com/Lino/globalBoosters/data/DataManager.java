package com.Lino.globalBoosters.data;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.ActiveBooster;
import com.Lino.globalBoosters.boosters.BoosterType;

import java.sql.*;
import java.util.UUID;

public class DataManager {

    private final GlobalBoosters plugin;
    private Connection connection;

    public DataManager(GlobalBoosters plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/active_boosters.db");

            String createActiveTable = "CREATE TABLE IF NOT EXISTS active_boosters (" +
                    "booster_type VARCHAR(50) PRIMARY KEY," +
                    "activator_uuid VARCHAR(36)," +
                    "activator_name VARCHAR(100)," +
                    "start_time BIGINT," +
                    "duration_minutes INTEGER," +
                    "remaining_seconds BIGINT)";

            String createStatsTable = "CREATE TABLE IF NOT EXISTS booster_stats (" +
                    "booster_type VARCHAR(50) PRIMARY KEY," +
                    "usage_count INTEGER DEFAULT 0)";

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createActiveTable);
                stmt.execute(createStatsTable);
            }

            for (BoosterType type : BoosterType.values()) {
                String insertDefault = "INSERT OR IGNORE INTO booster_stats (booster_type, usage_count) VALUES (?, 0)";
                try (PreparedStatement pstmt = connection.prepareStatement(insertDefault)) {
                    pstmt.setString(1, type.name());
                    pstmt.executeUpdate();
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Could not initialize database!");
            e.printStackTrace();
        }
    }

    public void saveActiveBooster(ActiveBooster booster) {
        String query = "INSERT OR REPLACE INTO active_boosters (booster_type, activator_uuid, activator_name, start_time, duration_minutes, remaining_seconds) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, booster.getType().name());
            pstmt.setString(2, booster.getActivatorUUID().toString());
            pstmt.setString(3, booster.getActivatorName());
            pstmt.setLong(4, booster.getStartTime());
            pstmt.setInt(5, booster.getDurationMinutes());
            pstmt.setLong(6, booster.getRemainingSeconds());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeActiveBooster(BoosterType type) {
        String query = "DELETE FROM active_boosters WHERE booster_type = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, type.name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void loadActiveBoosters() {
        String query = "SELECT * FROM active_boosters";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                try {
                    BoosterType type = BoosterType.valueOf(rs.getString("booster_type"));
                    UUID activatorUUID = UUID.fromString(rs.getString("activator_uuid"));
                    String activatorName = rs.getString("activator_name");
                    long startTime = rs.getLong("start_time");
                    int durationMinutes = rs.getInt("duration_minutes");
                    long remainingSeconds = rs.getLong("remaining_seconds");

                    long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
                    long totalDuration = durationMinutes * 60L;

                    if (elapsedTime < totalDuration) {
                        long adjustedRemaining = totalDuration - elapsedTime;

                        ActiveBooster booster = new ActiveBooster(
                                type,
                                activatorUUID,
                                activatorName,
                                startTime,
                                durationMinutes,
                                adjustedRemaining
                        );

                        plugin.getBoosterManager().loadBooster(booster);
                    } else {
                        removeActiveBooster(type);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load booster from database");
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void incrementBoosterUsage(BoosterType type) {
        String query = "UPDATE booster_stats SET usage_count = usage_count + 1 WHERE booster_type = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, type.name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ResultSet getTopBoosters(int limit) {
        String query = "SELECT booster_type, usage_count FROM booster_stats ORDER BY usage_count DESC LIMIT ?";

        try {
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, limit);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}