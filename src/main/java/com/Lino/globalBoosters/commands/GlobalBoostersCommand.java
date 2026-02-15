package com.Lino.globalBoosters.commands;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.ActiveBooster;
import com.Lino.globalBoosters.boosters.BoosterType;
import com.Lino.globalBoosters.utils.GradientColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

public class GlobalBoostersCommand implements CommandExecutor, TabCompleter {

    private final GlobalBoosters plugin;

    public GlobalBoostersCommand(GlobalBoosters plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sendActiveBoostersList(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            return handleReload(sender);
        }

        sendHelp(sender);
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

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.header"));
        sender.sendMessage("");
        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.boostshop"));
        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.globalboosters-help"));
        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.globalboosters-list"));

        if (sender.hasPermission("globalboosters.admin")) {
            sender.sendMessage("");
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.admin-header"));
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.booster-give"));
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.booster-start"));
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.booster-stop"));
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.booster-reload"));
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.booster-stats"));
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.booster-schedule"));
        }

        sender.sendMessage("");
        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.available-boosters"));
        sender.sendMessage("");

        for (BoosterType type : BoosterType.values()) {
            if (!plugin.getConfigManager().isBoosterEnabled(type)) {
                continue;
            }

            String boosterName = plugin.getMessagesManager().getBoosterName(type);
            String multiplier = "";
            if (!type.isEffectBooster() && !isNoMultiplierBooster(type)) {
                multiplier = GradientColor.apply(" <gradient:#808080:#A9A9A9>(" + plugin.getConfigManager().getBoosterMultiplier(type) + "x)</gradient>");
            }
            String typeName = GradientColor.apply("<gradient:#FFA500:#FFD700>" + type.name().toLowerCase() + "</gradient>");
            String separator = GradientColor.apply("<gradient:#808080:#A9A9A9> - </gradient>");
            sender.sendMessage(typeName + separator + boosterName + multiplier);
        }

        sender.sendMessage("");
        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.help.footer"));
    }

    private void sendActiveBoostersList(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.list.header"));
        sender.sendMessage("");

        if (plugin.getBoosterManager().getActiveBoosters().isEmpty()) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("commands.list.no-active"));
        } else {
            for (ActiveBooster booster : plugin.getBoosterManager().getActiveBoosters()) {
                String boosterName = plugin.getMessagesManager().getBoosterName(booster.getType());
                sender.sendMessage(boosterName);

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%player%", booster.getActivatorName());
                placeholders.put("%time%", booster.getTimeRemaining());

                sender.sendMessage("  " + plugin.getMessagesManager().getMessage("commands.list.activated-by", placeholders));
                sender.sendMessage("  " + plugin.getMessagesManager().getMessage("commands.list.time-remaining", placeholders));
                sender.sendMessage("");
            }
        }

        sender.sendMessage(plugin.getMessagesManager().getMessage("commands.list.footer"));
    }

    private boolean isNoMultiplierBooster(BoosterType type) {
        switch (type) {
            case NO_FALL_DAMAGE:
            case KEEP_INVENTORY:
            case FLY:
            case PLANT_GROWTH:
                return true;
            default:
                return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("help", "list"));
            if (sender.hasPermission("globalboosters.admin.reload")) {
                subCommands.add("reload");
            }
            return filterStartingWith(subCommands, args[0]);
        }
        return new ArrayList<>();
    }

    private List<String> filterStartingWith(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}