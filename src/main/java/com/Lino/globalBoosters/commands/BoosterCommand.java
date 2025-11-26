package com.Lino.globalBoosters.commands;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.BoosterType;
import com.Lino.globalBoosters.config.ConfigManager;
import com.Lino.globalBoosters.items.BoosterItem;
import com.Lino.globalBoosters.gui.BoosterShopGUI;
import com.Lino.globalBoosters.managers.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class BoosterCommand implements CommandExecutor, TabCompleter {

    private final GlobalBoosters plugin;

    public BoosterCommand(GlobalBoosters plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "give" -> handleGive(sender, args);
            case "start" -> handleStart(sender, args);
            case "stop" -> handleStop(sender, args);
            case "reload" -> handleReload(sender);
            case "stats" -> handleStats(sender);
            case "shop" -> handleShop(sender);
            case "schedule" -> handleSchedule(sender);
            case "random" -> handleRandom(sender);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("globalboosters.admin.give")) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("general.no-permission"));
            return true;
        }

        if (args.length < 3) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.booster.usage-give"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.booster.player-not-found"));
            return true;
        }

        BoosterType type;
        try {
            type = BoosterType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.booster.invalid-booster"));
            for (BoosterType boosterType : BoosterType.values()) {
                if (plugin.getConfigManager().isBoosterEnabled(boosterType)) {
                    plugin.getMessagesManager().sendMessage(sender, "<gradient:#808080:#A9A9A9>- </gradient><gradient:#FFA500:#FFD700>" + boosterType.name().toLowerCase() + "</gradient>");
                }
            }
            return true;
        }

        if (!plugin.getConfigManager().isBoosterEnabled(type)) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("booster.disabled"));
            return true;
        }

        int duration = plugin.getConfigManager().getBoosterDuration(type);
        if (args.length >= 4) {
            try {
                duration = Integer.parseInt(args[3]);
                if (duration <= 0) {
                    plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.booster.duration-positive"));
                    return true;
                }
            } catch (NumberFormatException e) {
                plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.booster.invalid-duration"));
                return true;
            }
        }

        ItemStack boosterItem = BoosterItem.createBoosterItem(type, duration);

        if (target.getInventory().firstEmpty() == -1) {
            target.getWorld().dropItem(target.getLocation(), boosterItem);
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("purchase.inventory-full"));
        } else {
            target.getInventory().addItem(boosterItem);
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(type));
        placeholders.put("%player%", target.getName());
        placeholders.put("%duration%", String.valueOf(duration));

        plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.booster.gave-booster", placeholders));
        plugin.getMessagesManager().sendMessage(target, plugin.getMessagesManager().getMessage("commands.booster.received-booster", placeholders));

        return true;
    }

    private boolean handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("globalboosters.admin")) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("general.no-permission"));
            return true;
        }

        if (args.length < 3) {
            plugin.getMessagesManager().sendMessage(sender, "<gradient:#FF0000:#FF6B6B>Usage: /booster start <type> <duration> [activator]</gradient>");
            return true;
        }

        BoosterType type;
        try {
            type = BoosterType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.booster.invalid-booster"));
            return true;
        }

        int duration;
        try {
            duration = Integer.parseInt(args[2]);
            if (duration <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.booster.invalid-duration"));
            return true;
        }

        String activatorName = "Console";
        if (args.length >= 4) {
            activatorName = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        }

        if (plugin.getBoosterManager().activateConsoleBooster(type, activatorName, duration)) {
            plugin.getMessagesManager().sendMessage(sender, "<gradient:#00FF00:#32CD32>Successfully started booster " + type.name() + "</gradient>");
        } else {
            plugin.getMessagesManager().sendMessage(sender, "<gradient:#FF0000:#FF6B6B>Failed to start booster (Active or Max Limit)</gradient>");
        }

        return true;
    }

    private boolean handleStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("globalboosters.admin")) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("general.no-permission"));
            return true;
        }

        if (args.length < 2) {
            plugin.getMessagesManager().sendMessage(sender, "<gradient:#FF0000:#FF6B6B>Usage: /booster stop <type></gradient>");
            return true;
        }

        BoosterType type;
        try {
            type = BoosterType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.booster.invalid-booster"));
            return true;
        }

        if (plugin.getBoosterManager().deactivateBooster(type)) {
            plugin.getMessagesManager().sendMessage(sender, "<gradient:#00FF00:#32CD32>Successfully stopped booster " + type.name() + "</gradient>");
        } else {
            plugin.getMessagesManager().sendMessage(sender, "<gradient:#FF0000:#FF6B6B>Booster " + type.name() + " is not active!</gradient>");
        }

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("globalboosters.admin.reload")) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("general.no-permission"));
            return true;
        }

        plugin.reloadConfig();
        plugin.getConfigManager().reload();
        plugin.getEconomyManager().reload();
        plugin.getMessagesManager().reload();
        plugin.reloadScheduledTask();
        LanguageManager.init(plugin);

        plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("general.reload-success"));
        return true;
    }

    private boolean handleShop(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("general.player-only"));
            return true;
        }

        if (!player.hasPermission("globalboosters.shop")) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("general.no-permission"));
            return true;
        }

        if (!plugin.getConfigManager().isShopGuiEnabled()) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("shop.disabled"));
            return true;
        }

        new BoosterShopGUI(plugin, player).open();
        return true;
    }

    private boolean handleStats(CommandSender sender) {
        if (!sender.hasPermission("globalboosters.admin.stats")) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("general.no-permission"));
            return true;
        }

        plugin.getMessagesManager().sendMessage(sender, "\n" + plugin.getMessagesManager().getMessage("commands.stats.header") + "\n");

        ResultSet rs = plugin.getDataManager().getTopBoosters(10);
        if (rs != null) {
            try {
                int position = 1;
                while (rs.next()) {
                    String boosterType = rs.getString("booster_type");
                    int usageCount = rs.getInt("usage_count");

                    if (usageCount > 0) {
                        try {
                            BoosterType type = BoosterType.valueOf(boosterType);
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("%position%", String.valueOf(position));
                            placeholders.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(type));
                            placeholders.put("%count%", String.valueOf(usageCount));

                            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.stats.entry", placeholders));
                            position++;
                        } catch (IllegalArgumentException e) {
                            continue;
                        }
                    }
                }

                if (position == 1) {
                    plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.stats.no-data"));
                }

                rs.close();
            } catch (SQLException e) {
                plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.stats.error"));
                e.printStackTrace();
            }
        } else {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.stats.error"));
        }

        plugin.getMessagesManager().sendMessage(sender, "\n" + plugin.getMessagesManager().getMessage("commands.stats.footer"));

        return true;
    }

    private boolean handleSchedule(CommandSender sender) {
        if (!sender.hasPermission("globalboosters.admin")) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("general.no-permission"));
            return true;
        }

        plugin.getMessagesManager().sendMessage(sender, "\n" + plugin.getMessagesManager().getMessage("commands.schedule.header") + "\n");

        if (!plugin.getConfigManager().isScheduledBoostersEnabled()) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.schedule.disabled") + "\n");
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.schedule.footer"));
            return true;
        }

        String timezoneStr = plugin.getConfigManager().getScheduledBoostersTimezone();
        ZoneId timezone;

        try {
            timezone = ZoneId.of(timezoneStr);
        } catch (Exception e) {
            timezone = ZoneId.systemDefault();
            timezoneStr = timezone.getId();
        }

        ZonedDateTime zonedNow = ZonedDateTime.now(timezone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String currentTime = zonedNow.format(formatter);
        String currentDay = zonedNow.getDayOfWeek().toString();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%timezone%", timezoneStr);
        placeholders.put("%time%", currentTime);
        placeholders.put("%day%", currentDay);

        plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.schedule.current-info", placeholders) + "\n");


        if (plugin.getConfigManager().getScheduledBoosters().isEmpty()) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.schedule.no-schedules"));
        } else {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.schedule.list-header"));
            for (ConfigManager.ScheduledBooster schedule : plugin.getConfigManager().getScheduledBoosters()) {
                placeholders.clear();
                placeholders.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(schedule.getType()));
                placeholders.put("%hour%", String.format("%02d", schedule.getHour()));
                placeholders.put("%minute%", String.format("%02d", schedule.getMinute()));
                placeholders.put("%duration%", String.valueOf(schedule.getDuration()));

                StringBuilder daysStr = new StringBuilder();
                for (DayOfWeek day : schedule.getDays()) {
                    if (!daysStr.isEmpty()) daysStr.append(", ");
                    daysStr.append(day.toString(), 0, 3);
                }
                placeholders.put("%days%", daysStr.toString());

                plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.schedule.entry", placeholders));
            }
        }

        plugin.getMessagesManager().sendMessage(sender, "\n" + plugin.getMessagesManager().getMessage("commands.schedule.footer"));
        return true;
    }

    private boolean handleRandom(CommandSender sender) {
        if (!sender.hasPermission("globalboosters.admin")) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("general.no-permission"));
            return true;
        }

        plugin.getMessagesManager().sendMessage(sender, "\n" + plugin.getMessagesManager().getMessage("commands.random.header") + "\n");

        if (!plugin.getConfigManager().isRandomScheduledEnabled()) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.random.disabled") + "\n");
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.random.footer"));
            return true;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%interval%", String.valueOf(plugin.getConfigManager().getRandomScheduledInterval()));
        placeholders.put("%duration%", String.valueOf(plugin.getConfigManager().getRandomScheduledDuration()));
        placeholders.put("%activator%", plugin.getConfigManager().getRandomScheduledActivatorName());

        plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.random.info", placeholders) + "\n");
        plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.random.pool-header"));

        List<String> boosterPool = plugin.getConfigManager().getRandomScheduledBoosters();
        for (String boosterName : boosterPool) {
            try {
                BoosterType type = BoosterType.valueOf(boosterName.toUpperCase());
                if (plugin.getConfigManager().isBoosterEnabled(type)) {
                    plugin.getMessagesManager().sendMessage(sender, "<gradient:#808080:#A9A9A9>• </gradient>" + plugin.getMessagesManager().getBoosterName(type));
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        plugin.getMessagesManager().sendMessage(sender, "\n" + plugin.getMessagesManager().getMessage("commands.random.footer"));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.booster.help-header"));
        plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.help.booster-shop"));
        if (sender.hasPermission("globalboosters.admin.give")) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.help.booster-give"));
        }
        if (sender.hasPermission("globalboosters.admin.reload")) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.help.booster-reload"));
        }
        if (sender.hasPermission("globalboosters.admin.stats")) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.help.booster-stats"));
        }
        if (sender.hasPermission("globalboosters.admin")) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.help.booster-start"));
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.help.booster-stop"));
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.help.booster-schedule"));
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.help.booster-random"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            subCommands.add("shop");
            if (sender.hasPermission("globalboosters.admin.give")) {
                subCommands.add("give");
            }
            if (sender.hasPermission("globalboosters.admin")) {
                subCommands.add("start");
                subCommands.add("stop");
            }
            if (sender.hasPermission("globalboosters.admin.reload")) {
                subCommands.add("reload");
            }
            if (sender.hasPermission("globalboosters.admin.stats")) {
                subCommands.add("stats");
            }
            if (sender.hasPermission("globalboosters.admin")) {
                subCommands.add("schedule");
                subCommands.add("random");
            }
            return filterStartingWith(subCommands, args[0]);
        }

        if (args[0].equalsIgnoreCase("give") && sender.hasPermission("globalboosters.admin.give")) {
            if (args.length == 2) {
                return filterStartingWith(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList()), args[1]);
            } else if (args.length == 3) {
                return getEnabledBoosters(args[2]);
            } else if (args.length == 4) {
                return Arrays.asList("30", "60", "120");
            }
        }

        if (args[0].equalsIgnoreCase("start") && sender.hasPermission("globalboosters.admin")) {
            if (args.length == 2) {
                return getEnabledBoosters(args[1]);
            } else if (args.length == 3) {
                return Arrays.asList("30", "60", "120");
            } else if (args.length == 4) {
                return Collections.singletonList("Console");
            }
        }

        if (args[0].equalsIgnoreCase("stop") && sender.hasPermission("globalboosters.admin")) {
            if (args.length == 2) {
                return filterStartingWith(plugin.getBoosterManager().getActiveBoosters().stream()
                        .map(b -> b.getType().name().toLowerCase())
                        .collect(Collectors.toList()), args[1]);
            }
        }

        return new ArrayList<>();
    }

    private List<String> getEnabledBoosters(String prefix) {
        List<String> enabledBoosters = new ArrayList<>();
        for (BoosterType type : BoosterType.values()) {
            if (plugin.getConfigManager().isBoosterEnabled(type)) {
                enabledBoosters.add(type.name().toLowerCase());
            }
        }
        return filterStartingWith(enabledBoosters, prefix);
    }

    private List<String> filterStartingWith(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
