package com.Lino.globalBoosters.commands;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.BoosterType;
import com.Lino.globalBoosters.config.ConfigManager;
import com.Lino.globalBoosters.items.BoosterItem;
import com.Lino.globalBoosters.gui.BoosterShopGUI;
import com.Lino.globalBoosters.utils.GradientColor;
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

        switch (subCommand) {
            case "give":
                return handleGive(sender, args);
            case "start":
                return handleStart(sender, args);
            case "stop":
                return handleStop(sender, args);
            case "reload":
                return handleReload(sender);
            case "stats":
                return handleStats(sender);
            case "shop":
                return handleShop(sender);
            case "schedule":
                return handleSchedule(sender);
            case "random":
                return handleRandom(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("globalboosters.admin.give")) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("general.no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.booster.usage-give"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.booster.player-not-found"));
            return true;
        }

        BoosterType type;
        try {
            type = BoosterType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.booster.invalid-booster"));
            for (BoosterType boosterType : BoosterType.values()) {
                if (plugin.getConfigManager().isBoosterEnabled(boosterType)) {
                    sender.sendMessage(GradientColor.apply("<gradient:#808080:#A9A9A9>- </gradient><gradient:#FFA500:#FFD700>" + boosterType.name().toLowerCase() + "</gradient>"));
                }
            }
            return true;
        }

        if (!plugin.getConfigManager().isBoosterEnabled(type)) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("booster.disabled"));
            return true;
        }

        int duration = plugin.getConfigManager().getBoosterDuration(type);
        if (args.length >= 4) {
            try {
                duration = Integer.parseInt(args[3]);
                if (duration <= 0) {
                    sender.sendMessage(plugin.getMessagesManager().getMessage("commands.booster.duration-positive"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getMessagesManager().getMessage("commands.booster.invalid-duration"));
                return true;
            }
        }

        ItemStack boosterItem = BoosterItem.createBoosterItem(type, duration);

        if (target.getInventory().firstEmpty() == -1) {
            target.getWorld().dropItem(target.getLocation(), boosterItem);
            target.sendMessage(plugin.getMessagesManager().getMessage("purchase.inventory-full"));
        } else {
            target.getInventory().addItem(boosterItem);
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(type));
        placeholders.put("%player%", target.getName());
        placeholders.put("%duration%", String.valueOf(duration));

        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.booster.gave-booster", placeholders));
        target.sendMessage(plugin.getMessagesManager().getMessage("commands.booster.received-booster", placeholders));

        return true;
    }

    private boolean handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("globalboosters.admin")) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("general.no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(GradientColor.apply("<gradient:#FF0000:#FF6B6B>Usage: /booster start <type> <duration> [activator]</gradient>"));
            return true;
        }

        BoosterType type;
        try {
            type = BoosterType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.booster.invalid-booster"));
            return true;
        }

        int duration;
        try {
            duration = Integer.parseInt(args[2]);
            if (duration <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.booster.invalid-duration"));
            return true;
        }

        String activatorName = "Console";
        if (args.length >= 4) {
            activatorName = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        }

        if (plugin.getBoosterManager().activateConsoleBooster(type, activatorName, duration)) {
            sender.sendMessage(GradientColor.apply("<gradient:#00FF00:#32CD32>Successfully started booster " + type.name() + "</gradient>"));
        } else {
            sender.sendMessage(GradientColor.apply("<gradient:#FF0000:#FF6B6B>Failed to start booster (Active or Max Limit)</gradient>"));
        }

        return true;
    }

    private boolean handleStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("globalboosters.admin")) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("general.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(GradientColor.apply("<gradient:#FF0000:#FF6B6B>Usage: /booster stop <type></gradient>"));
            return true;
        }

        BoosterType type;
        try {
            type = BoosterType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.booster.invalid-booster"));
            return true;
        }

        if (plugin.getBoosterManager().deactivateBooster(type)) {
            sender.sendMessage(GradientColor.apply("<gradient:#00FF00:#32CD32>Successfully stopped booster " + type.name() + "</gradient>"));
        } else {
            sender.sendMessage(GradientColor.apply("<gradient:#FF0000:#FF6B6B>Booster " + type.name() + " is not active!</gradient>"));
        }

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("globalboosters.admin.reload")) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("general.no-permission"));
            return true;
        }

        plugin.performReload();

        sender.sendMessage(plugin.getMessagesManager().getMessage("general.reload-success"));
        return true;
    }

    private boolean handleShop(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("general.player-only"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("globalboosters.shop")) {
            player.sendMessage(plugin.getMessagesManager().getMessage("general.no-permission"));
            return true;
        }

        if (!plugin.getConfigManager().isShopGuiEnabled()) {
            player.sendMessage(plugin.getMessagesManager().getMessage("shop.disabled"));
            return true;
        }

        new BoosterShopGUI(plugin, player).open();
        return true;
    }

    private boolean handleStats(CommandSender sender) {
        if (!sender.hasPermission("globalboosters.admin.stats")) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("general.no-permission"));
            return true;
        }

        sender.sendMessage("");
        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.stats.header"));
        sender.sendMessage("");

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

                            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.stats.entry", placeholders));
                            position++;
                        } catch (IllegalArgumentException e) {
                            continue;
                        }
                    }
                }

                if (position == 1) {
                    sender.sendMessage(plugin.getMessagesManager().getMessage("commands.stats.no-data"));
                }

                rs.close();
            } catch (SQLException e) {
                sender.sendMessage(plugin.getMessagesManager().getMessage("commands.stats.error"));
                e.printStackTrace();
            }
        } else {
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.stats.error"));
        }

        sender.sendMessage("");
        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.stats.footer"));

        return true;
    }

    private boolean handleSchedule(CommandSender sender) {
        if (!sender.hasPermission("globalboosters.admin")) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("general.no-permission"));
            return true;
        }

        sender.sendMessage("");
        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.schedule.header"));
        sender.sendMessage("");

        if (!plugin.getConfigManager().isScheduledBoostersEnabled()) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.schedule.disabled"));
            sender.sendMessage("");
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.schedule.footer"));
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
        String currentDay = zonedNow.getDayOfWeek().name();

        Map<String, String> infoPlaceholders = new HashMap<>();
        infoPlaceholders.put("%timezone%", timezoneStr);
        infoPlaceholders.put("%time%", currentTime);
        infoPlaceholders.put("%day%", currentDay);
        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.schedule.current-info", infoPlaceholders));
        sender.sendMessage("");

        List<ConfigManager.ScheduledBooster> schedules = plugin.getConfigManager().getScheduledBoosters();
        if (schedules.isEmpty()) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.schedule.no-schedules"));
        } else {
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.schedule.list-header"));
            sender.sendMessage("");
            for (ConfigManager.ScheduledBooster schedule : schedules) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(schedule.getType()));
                placeholders.put("%hour%", String.format("%02d", schedule.getHour()));
                placeholders.put("%minute%", String.format("%02d", schedule.getMinute()));
                placeholders.put("%duration%", String.valueOf(schedule.getDuration()));
                placeholders.put("%days%", schedule.getDays().stream()
                        .map(DayOfWeek::name)
                        .collect(Collectors.joining(", ")));
                sender.sendMessage(plugin.getMessagesManager().getMessage("commands.schedule.entry", placeholders));
            }
        }

        sender.sendMessage("");
        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.schedule.footer"));
        return true;
    }

    private boolean handleRandom(CommandSender sender) {
        if (!sender.hasPermission("globalboosters.admin")) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("general.no-permission"));
            return true;
        }

        sender.sendMessage("");
        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.random.header"));
        sender.sendMessage("");

        if (!plugin.getConfigManager().isRandomScheduledEnabled()) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.random.disabled"));
            sender.sendMessage("");
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.random.footer"));
            return true;
        }

        Map<String, String> infoPlaceholders = new HashMap<>();
        infoPlaceholders.put("%interval%", String.valueOf(plugin.getConfigManager().getRandomScheduledInterval()));
        infoPlaceholders.put("%duration%", String.valueOf(plugin.getConfigManager().getRandomScheduledDuration()));
        infoPlaceholders.put("%activator%", plugin.getConfigManager().getRandomScheduledActivatorName());
        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.random.info", infoPlaceholders));
        sender.sendMessage("");

        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.random.pool-header"));
        for (String boosterName : plugin.getConfigManager().getRandomScheduledBoosters()) {
            try {
                BoosterType type = BoosterType.valueOf(boosterName.toUpperCase());
                sender.sendMessage(GradientColor.apply("<gradient:#808080:#A9A9A9>- </gradient>") + plugin.getMessagesManager().getBoosterName(type));
            } catch (IllegalArgumentException e) {
                sender.sendMessage(GradientColor.apply("<gradient:#808080:#A9A9A9>- </gradient><gradient:#FF0000:#FF6B6B>" + boosterName + " (invalid)</gradient>"));
            }
        }

        sender.sendMessage("");
        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.random.footer"));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.booster.help-header"));
        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.booster-shop"));
        if (sender.hasPermission("globalboosters.admin.give")) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.booster-give"));
        }
        if (sender.hasPermission("globalboosters.admin.reload")) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.booster-reload"));
        }
        if (sender.hasPermission("globalboosters.admin.stats")) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.booster-stats"));
        }
        if (sender.hasPermission("globalboosters.admin")) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.booster-start"));
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.booster-stop"));
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.booster-schedule"));
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.booster-random"));
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

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                return null;
            }
            if (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("stop")) {
                List<String> types = new ArrayList<>();
                for (BoosterType type : BoosterType.values()) {
                    if (plugin.getConfigManager().isBoosterEnabled(type)) {
                        types.add(type.name().toLowerCase());
                    }
                }
                return filterStartingWith(types, args[1]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> types = new ArrayList<>();
            for (BoosterType type : BoosterType.values()) {
                if (plugin.getConfigManager().isBoosterEnabled(type)) {
                    types.add(type.name().toLowerCase());
                }
            }
            return filterStartingWith(types, args[2]);
        }

        return new ArrayList<>();
    }

    private List<String> filterStartingWith(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}