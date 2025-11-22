package com.Lino.globalBoosters.gui;

import com.Lino.globalBoosters.GlobalBoosters;
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

public class ConfirmExchangeGUI {

    private final GlobalBoosters plugin;
    private final Player player;
    private final Material material;
    private final int requiredAmount;
    private final CoinExchangeGUI parentGUI;
    private final Inventory inventory;

    public ConfirmExchangeGUI(GlobalBoosters plugin, Player player, Material material, int requiredAmount, CoinExchangeGUI parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.material = material;
        this.requiredAmount = requiredAmount;
        this.parentGUI = parentGUI;
        this.inventory = Bukkit.createInventory(null, 27,
                plugin.getMessagesManager().getMessageComponent("coin-exchange.confirm-title", null));

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
        placeholders.put("%material%", getMaterialDisplayName(material));
        placeholders.put("%amount%", String.valueOf(requiredAmount));

        ItemStack confirmItem = new ItemBuilder(Material.LIME_WOOL)
                .setDisplayName(plugin.getMessagesManager().getMessage("coin-exchange.confirm-button"))
                .setLore(plugin.getMessagesManager().getMessageList("coin-exchange.confirm-exchange", placeholders))
                .build();

        ItemStack cancelItem = new ItemBuilder(Material.RED_WOOL)
                .setDisplayName(plugin.getMessagesManager().getMessage("coin-exchange.cancel-button"))
                .setLore(Arrays.asList(
                        "",
                        plugin.getMessagesManager().getMessage("coin-exchange.return-to-exchange"),
                        "",
                        plugin.getMessagesManager().getMessage("coin-exchange.click-cancel")
                ))
                .build();

        ItemStack exchangeItem = new ItemBuilder(material)
                .setAmount(requiredAmount)
                .setDisplayName(plugin.getMessagesManager().getMessage("coin-exchange.exchange-item-name", placeholders))
                .setLore(Arrays.asList(
                        "",
                        plugin.getMessagesManager().getMessage("coin-exchange.exchange-for-coin")
                ))
                .addGlow(true)
                .build();

        inventory.setItem(11, confirmItem);
        inventory.setItem(13, exchangeItem);
        inventory.setItem(15, cancelItem);
    }

    private String getMaterialDisplayName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public void open() {
        player.openInventory(inventory);
        playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f);

        BoosterItemListener listener = getBoosterItemListener();
        if (listener != null) {
            listener.unregisterGUIs(player);
            listener.registerConfirmExchangeGUI(player, this);
        }
    }

    public void handleClick(int slot) {
        if (slot == 11) {
            parentGUI.executeExchange(material, requiredAmount);
            player.closeInventory();
        } else if (slot == 15) {
            parentGUI.open();
        }
    }

    private void playSound(Sound sound, float volumeMultiplier) {
        float volume = (float) plugin.getConfigManager().getSoundVolume() * volumeMultiplier;
        if (volume > 0) {
            player.playSound(player.getLocation(), sound, volume, 1.0f);
        }
    }

    public Inventory getInventory() {
        return inventory;
    }

    private BoosterItemListener getBoosterItemListener() {
        return plugin.getBoosterItemListener();
    }
}
