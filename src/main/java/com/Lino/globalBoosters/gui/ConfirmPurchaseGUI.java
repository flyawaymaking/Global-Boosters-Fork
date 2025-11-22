package com.Lino.globalBoosters.gui;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.BoosterType;
import com.Lino.globalBoosters.economy.EconomyProvider;
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
    private final String providerKey;
    private final Inventory inventory;

    public ConfirmPurchaseGUI(GlobalBoosters plugin, Player player, BoosterType boosterType, double price, String providerKey) {
        this.plugin = plugin;
        this.player = player;
        this.boosterType = boosterType;
        this.price = price;
        this.providerKey = providerKey;
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

        EconomyProvider provider = getProvider();
        if (provider == null) {
            provider = plugin.getEconomyManager().getDefaultProvider();
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(boosterType));
        placeholders.put("%price%", provider.format(price));

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
        playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);

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
            new BoosterShopGUI(plugin, player, providerKey).open();
        }
    }

    private void purchaseBooster() {
        EconomyProvider provider = getProvider();
        if (provider == null) {
            plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("purchase.economy-unavailable"));
            playSound(Sound.ENTITY_VILLAGER_NO, 1f, 1.0f);
            player.closeInventory();
            return;
        }

        if (!provider.hasEnough(player, price)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%price%", provider.format(price));
            plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("purchase.not-enough-money", placeholders));
            playSound(Sound.ENTITY_VILLAGER_NO, 1f, 1.0f);
            player.closeInventory();
            return;
        }

        if (plugin.getConfigManager().isLimitedSupplyEnabled() && !plugin.getSupplyManager().canPurchase(boosterType)) {
            plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("purchase.out-of-stock",
                    Map.of("%booster%", plugin.getMessagesManager().getBoosterNameRaw(boosterType))));
            playSound(Sound.ENTITY_VILLAGER_NO, 1f, 1.0f);
            player.closeInventory();
            return;
        }

        if (!provider.withdraw(player, price)) {
            plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("purchase.transaction-failed"));
            playSound(Sound.ENTITY_VILLAGER_NO, 1f, 1.0f);
            player.closeInventory();
            return;
        }

        if (plugin.getConfigManager().isLimitedSupplyEnabled()) {
            plugin.getSupplyManager().recordPurchase(boosterType);
        }

        ItemStack boosterItem = BoosterItem.createBoosterItem(
                boosterType,
                plugin.getConfigManager().getBoosterDuration(boosterType)
        );

        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), boosterItem);
            plugin.getMessagesManager().sendMessage(player, "purchase.inventory-full");
        } else {
            player.getInventory().addItem(boosterItem);
        }

        Map<String, String> successPlaceholders = new HashMap<>();
        successPlaceholders.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(boosterType));
        successPlaceholders.put("%price%", provider.format(price));

        plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("purchase.success", successPlaceholders));
        playSound(Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.0f);
        player.closeInventory();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            new BoosterShopGUI(plugin, player, providerKey).open();
        }, 1L);
    }

    private EconomyProvider getProvider() {
        return plugin.getEconomyManager().getProvider(providerKey);
    }

    private void playSound(Sound sound, float volumeMultiplier, float pitch) {
        float volume = (float) plugin.getConfigManager().getSoundVolume() * volumeMultiplier;
        if (volume > 0) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    public Inventory getInventory() {
        return inventory;
    }

    private BoosterItemListener getBoosterItemListener() {
        return plugin.getBoosterItemListener();
    }
}
