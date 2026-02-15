package com.Lino.globalBoosters.hooks;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.ActiveBooster;
import com.Lino.globalBoosters.boosters.BoosterType;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.stream.Collectors;

public class BoosterPlaceholderExpansion extends PlaceholderExpansion {

    private final GlobalBoosters plugin;

    public BoosterPlaceholderExpansion(GlobalBoosters plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "globalboosters";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.equalsIgnoreCase("active_count")) {
            return String.valueOf(plugin.getBoosterManager().getActiveBoosterCount());
        }

        if (params.equalsIgnoreCase("active_list")) {
            Collection<ActiveBooster> boosters = plugin.getBoosterManager().getActiveBoosters();
            if (boosters.isEmpty()) {
                return "None";
            }
            return boosters.stream()
                    .map(b -> b.getType().getDisplayName())
                    .collect(Collectors.joining(", "));
        }

        if (params.equalsIgnoreCase("max_active")) {
            return String.valueOf(plugin.getConfigManager().getMaxActiveBoosters());
        }

        for (BoosterType type : BoosterType.values()) {
            String typeLower = type.name().toLowerCase();

            if (params.equalsIgnoreCase(typeLower + "_active")) {
                return String.valueOf(plugin.getBoosterManager().isBoosterActive(type));
            }

            if (params.equalsIgnoreCase(typeLower + "_time")) {
                ActiveBooster booster = plugin.getBoosterManager().getActiveBooster(type);
                if (booster == null) {
                    return "Inactive";
                }
                return booster.getTimeRemaining();
            }

            if (params.equalsIgnoreCase(typeLower + "_time_seconds")) {
                ActiveBooster booster = plugin.getBoosterManager().getActiveBooster(type);
                if (booster == null) {
                    return "0";
                }
                return String.valueOf(booster.getRemainingSeconds());
            }

            if (params.equalsIgnoreCase(typeLower + "_multiplier")) {
                return String.valueOf(plugin.getBoosterManager().getMultiplier(type));
            }

            if (params.equalsIgnoreCase(typeLower + "_activator")) {
                ActiveBooster booster = plugin.getBoosterManager().getActiveBooster(type);
                if (booster == null) {
                    return "";
                }
                return booster.getActivatorName();
            }
        }

        return null;
    }
}