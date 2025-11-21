package com.Lino.globalBoosters.gui;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.BoosterType;
import com.Lino.globalBoosters.items.BoosterItem;
import com.Lino.globalBoosters.listeners.BoosterItemListener;
import com.Lino.globalBoosters.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ConfirmPurchaseGUI {

    private final GlobalBoosters plugin;
    private final Player player;
    private final BoosterType boosterType;
    private final double price;
    private final Inventory inventory;

    public ConfirmPurchaseGUI(GlobalBoosters plugin, Player player, BoosterType boosterType, double price) {
        this.plugin = plugin;
        this.player = player;
        this.boosterType = boosterType;
        this.price = price;
        this.inventory = Bukkit.createInventory(null, 27, plugin.getMessagesManager().getMessageComponent("shop.confirm-title", null));

        setupGUI();
    }

    private void setupGUI() {
        ItemStack grayGlass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setDisplayName(" ")
                .build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, grayGlass);
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(boosterType));
        placeholders.put("%price%", String.format("%.2f", price));
        ItemStack confirmItem = new ItemBuilder(Material.LIME_WOOL)
                .setDisplayName(plugin.getMessagesManager().getMessage("shop.confirm.button"))
                .setLore(Arrays.asList(
                        "",
                        plugin.getMessagesManager().getMessage("shop.confirm.lore-booster", placeholders),
                        plugin.getMessagesManager().getMessage("shop.confirm.lore-price", placeholders),
                        "",
                        plugin.getMessagesManager().getMessage("shop.confirm.click-confirm")
                ))
                .build();
        ItemStack cancelItem = new ItemBuilder(Material.RED_WOOL)
                .setDisplayName(plugin.getMessagesManager().getMessage("shop.confirm.cancel"))
                .setLore(Arrays.asList(
                        "",
                        plugin.getMessagesManager().getMessage("shop.confirm.return-shop"),
                        "",
                        plugin.getMessagesManager().getMessage("shop.confirm.click-cancel")
                ))
                .build();
        ItemStack infoItem = new ItemBuilder(boosterType.getIcon())
                .setDisplayName(plugin.getMessagesManager().getMessage("shop.confirm.info-title", placeholders))
                .setLore(Arrays.asList(
                        "",
                        plugin.getMessagesManager().getMessage("shop.confirm.info-lore.line1"),
                        plugin.getMessagesManager().getMessage("shop.confirm.info-lore.line2"),
                        plugin.getMessagesManager().getMessage("shop.confirm.info-lore.line3"),
                        plugin.getMessagesManager().getMessage("shop.confirm.info-lore.line4"),
                        plugin.getMessagesManager().getMessage("shop.confirm.info-lore.line5")
                ))
                .addGlow(true)
                .build();
        inventory.setItem(11, confirmItem);
        inventory.setItem(13, infoItem);
        inventory.setItem(15, cancelItem);
    }

    public void open() {
        player.openInventory(inventory);
        float volume = (float) (plugin.getConfigManager().getSoundVolume() * 0.5f);
        if (volume > 0) player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, volume, 1.5f);

        BoosterItemListener listener = getBoosterItemListener();
        if (listener != null) {
            listener.unregisterGUIs(player);
            listener.registerConfirmGUI(player, this);
        }
    }

    public void handleClick(int slot) {
        if (slot == 11) {
            purchaseBooster();
        } else if (slot == 15) {
            player.closeInventory();
            new BoosterShopGUI(plugin, player).open();
        }
    }

    private void purchaseBooster() {
        float volume = (float) plugin.getConfigManager().getSoundVolume();

        if (!plugin.getEconomy().has(player, price)) {
            plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("purchase.not-enough-money",
                    new HashMap<String, String>() {{
                        put("%price%", String.format("%.2f", price));
                    }}));
            if (volume > 0) player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, volume, 1.0f);
            player.closeInventory();
            return;
        }

        if (plugin.getConfigManager().isLimitedSupplyEnabled()) {
            if (!plugin.getSupplyManager().canPurchase(boosterType)) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(boosterType));
                plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("purchase.out-of-stock", placeholders));
                if (volume > 0) player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, volume, 1.0f);
                player.closeInventory();
                return;
            }
        }

        plugin.getEconomy().withdrawPlayer(player, price);
        if (plugin.getConfigManager().isLimitedSupplyEnabled()) {
            if (!plugin.getSupplyManager().recordPurchase(boosterType)) {
                plugin.getEconomy().depositPlayer(player, price);
                Map<String, String> placeholders2 = new HashMap<>();
                placeholders2.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(boosterType));
                plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("purchase.out-of-stock", placeholders2));
                if (volume > 0) player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, volume, 1.0f);
                player.closeInventory();
                return;
            }
        }

        ItemStack boosterItem = BoosterItem.createBoosterItem(
                boosterType,
                plugin.getConfigManager().getBoosterDuration(boosterType)
        );
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), boosterItem);
            plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("purchase.inventory-full"));
        } else {
            player.getInventory().addItem(boosterItem);
        }

        Map<String, String> placeholders3 = new HashMap<>();
        placeholders3.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(boosterType));
        placeholders3.put("%price%", String.format("%.2f", price));
        plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("purchase.success", placeholders3));
        if (volume > 0) player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, volume, 1.0f);
        player.closeInventory();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            new BoosterShopGUI(plugin, player).open();
        }, 1L);
    }

    public Inventory getInventory() {
        return inventory;
    }

    private BoosterItemListener getBoosterItemListener() {
        return plugin.getBoosterItemListener();
    }
}
