package com.Lino.globalBoosters.data;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.BoosterType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BoosterLogger {

    private final GlobalBoosters plugin;
    private final File logFile;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public BoosterLogger(GlobalBoosters plugin) {
        this.plugin = plugin;
        File logsFolder = new File(plugin.getDataFolder(), "logs");
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }
        this.logFile = new File(logsFolder, "booster_history.txt");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
                writeHeader();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create booster_history.txt!");
            }
        }
    }

    private void writeHeader() {
        writeLine("========================================");
        writeLine("  GlobalBoosters - Activation History");
        writeLine("========================================");
        writeLine("");
    }

    public void logActivation(BoosterType type, String activatorName, int durationMinutes, String source) {
        if (!plugin.getConfigManager().isHistoryLogEnabled()) {
            return;
        }
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String line = "[" + timestamp + "] ACTIVATED | Type: " + type.name()
                + " | By: " + activatorName
                + " | Duration: " + durationMinutes + "min"
                + " | Source: " + source;
        writeLine(line);
    }

    public void logDeactivation(BoosterType type, String reason) {
        if (!plugin.getConfigManager().isHistoryLogEnabled()) {
            return;
        }
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String line = "[" + timestamp + "] EXPIRED   | Type: " + type.name()
                + " | Reason: " + reason;
        writeLine(line);
    }

    public void logQueue(BoosterType type, String activatorName, int durationMinutes) {
        if (!plugin.getConfigManager().isHistoryLogEnabled()) {
            return;
        }
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String line = "[" + timestamp + "] QUEUED    | Type: " + type.name()
                + " | By: " + activatorName
                + " | Duration: " + durationMinutes + "min";
        writeLine(line);
    }

    private void writeLine(String line) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write to booster_history.txt: " + e.getMessage());
        }
    }
}