package com.Lino.globalBoosters.commands;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.ActiveBooster;
import com.Lino.globalBoosters.boosters.BoosterType;
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

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        plugin.getMessagesManager().sendMessage(sender, "\n" + plugin.getMessagesManager().getMessage("commands.help.header") + "\n");
        plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.help.boostshop"));
        plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.help.globalboosters-help"));
        plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.help.globalboosters-list"));

        if (sender.hasPermission("globalboosters.admin")) {
            plugin.getMessagesManager().sendMessage(sender, "\n" + plugin.getMessagesManager().getMessage("commands.help.admin-header"));
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.help.booster-give"));
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.help.booster-start"));
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.help.booster-stop"));
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.help.booster-reload"));
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.help.booster-stats"));
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.help.booster-schedule"));
        }

        plugin.getMessagesManager().sendMessage(sender, "\n" + plugin.getMessagesManager().getMessage("commands.help.available-boosters") + "\n");

        for (BoosterType type : BoosterType.values()) {
            if (!plugin.getConfigManager().isBoosterEnabled(type)) {
                continue;
            }

            String boosterName = plugin.getMessagesManager().getBoosterName(type);
            String multiplier = "";
            if (!type.isEffectBooster() && !isNoMultiplierBooster(type)) {
                multiplier = " <gradient:#808080:#A9A9A9>(" + plugin.getConfigManager().getBoosterMultiplier(type) + "x)</gradient>";
            }
            String typeName = "<gradient:#FFA500:#FFD700>" + type.name().toLowerCase() + "</gradient>";
            String separator = "<gradient:#808080:#A9A9A9> - </gradient>";
            plugin.getMessagesManager().sendMessage(sender, typeName + separator + boosterName + multiplier);
        }

        plugin.getMessagesManager().sendMessage(sender, "\n" + plugin.getMessagesManager().getMessage("commands.help.footer"));
    }

    private void sendActiveBoostersList(CommandSender sender) {
        plugin.getMessagesManager().sendMessage(sender, "\n" + plugin.getMessagesManager().getMessage("commands.list.header") + "\n");

        if (plugin.getBoosterManager().getActiveBoosters().isEmpty()) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.list.no-active"));
        } else {
            for (ActiveBooster booster : plugin.getBoosterManager().getActiveBoosters()) {
                String boosterName = plugin.getMessagesManager().getBoosterName(booster.getType());
                plugin.getMessagesManager().sendMessage(sender, boosterName);

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%player%", booster.getActivatorName());
                placeholders.put("%time%", booster.getTimeRemaining());

                plugin.getMessagesManager().sendMessage(sender, "  " + plugin.getMessagesManager().getMessage("commands.list.activated-by", placeholders));
                plugin.getMessagesManager().sendMessage(sender, "  " + plugin.getMessagesManager().getMessage("commands.list.time-remaining", placeholders) + "\n");
            }
        }

        plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("commands.list.footer"));
    }

    private boolean isNoMultiplierBooster(BoosterType type) {
        return switch (type) {
            case NO_FALL_DAMAGE, KEEP_INVENTORY, FLY, PLANT_GROWTH -> true;
            default -> false;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("help", "list");
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
