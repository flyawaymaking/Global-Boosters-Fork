package com.Lino.globalBoosters.commands;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.gui.BoosterShopGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BoostShopCommand implements CommandExecutor, TabCompleter {

    private final GlobalBoosters plugin;

    public BoostShopCommand(GlobalBoosters plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessagesManager().sendMessage(sender, plugin.getMessagesManager().getMessage("general.player-only"));
            return true;
        }

        if (!player.hasPermission("globalboosters.shop")) {
            plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("general.no-permission"));
            return true;
        }

        if (!plugin.getConfigManager().isShopGuiEnabled()) {
            plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("shop.disabled"));
            return true;
        }

        new BoosterShopGUI(plugin, player).open();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
