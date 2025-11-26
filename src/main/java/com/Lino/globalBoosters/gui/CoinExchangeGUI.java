package com.Lino.globalBoosters.gui;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.economy.BoosterCoinProvider;
import com.Lino.globalBoosters.listeners.BoosterItemListener;
import com.Lino.globalBoosters.managers.LanguageManager;
import com.Lino.globalBoosters.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class CoinExchangeGUI {

    private final GlobalBoosters plugin;
    private final Player player;
    private final Inventory inventory;
    private final BoosterCoinProvider coinProvider;

    private final int[] EXCHANGE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43
    };
    private final int BACK_BUTTON_SLOT = 49;
    private final int INFO_SLOT = 4;

    public CoinExchangeGUI(GlobalBoosters plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.coinProvider = (BoosterCoinProvider) plugin.getEconomyManager().getProvider("boostercoins");
        this.inventory = Bukkit.createInventory(null, 54,
                plugin.getMessagesManager().getMessageComponent("coin-exchange.title", null));

        setupGUI();
    }

    private void setupGUI() {
        fillDecoration();
        setupExchangeItems();
        setupInfoItem();
        setupInstructions();
    }

    private void fillDecoration() {
        ItemStack decorationItem = new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE)
                .setDisplayName(" ")
                .build();

        for (int i = 0; i < 54; i++) {
            if (i < 9 || i > 44 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, decorationItem);
            }
        }
    }

    private void setupExchangeItems() {
        Map<String, Integer> exchangeRates = coinProvider.getExchangeRates();
        List<Map.Entry<String, Integer>> ratesList = new ArrayList<>(exchangeRates.entrySet());

        for (int i = 0; i < ratesList.size() && i < EXCHANGE_SLOTS.length; i++) {
            Map.Entry<String, Integer> entry = ratesList.get(i);
            Material material = Material.getMaterial(entry.getKey());
            if (material != null) {
                int requiredAmount = entry.getValue();
                inventory.setItem(EXCHANGE_SLOTS[i], createExchangeItem(material, requiredAmount));
            }
        }
    }

    private ItemStack createExchangeItem(Material material, int requiredAmount) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%material%", getMaterialDisplayName(material));
        placeholders.put("%amount%", String.valueOf(requiredAmount));

        int playerAmount = countPlayerItems(material);
        boolean canExchange = playerAmount >= requiredAmount;

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(plugin.getMessagesManager().getMessage("coin-exchange.exchange-rate", placeholders));
        lore.add(plugin.getMessagesManager().getMessage("coin-exchange.your-items",
                Map.of("%current%", String.valueOf(playerAmount))));
        lore.add("");

        if (canExchange) {
            lore.add(plugin.getMessagesManager().getMessage("coin-exchange.click-to-exchange"));
        } else {
            lore.add(plugin.getMessagesManager().getMessage("coin-exchange.not-enough-items-inv"));
        }

        Material displayMaterial = canExchange ? material : Material.BARRIER;

        return new ItemBuilder(displayMaterial)
                .setAmount(requiredAmount)
                .setDisplayName(plugin.getMessagesManager().getMessage("coin-exchange.item-name",
                        Map.of("%material%", getMaterialDisplayName(material))))
                .setLore(lore)
                .build();
    }

    private void setupInfoItem() {
        int currentCoins = (int) coinProvider.getBalance(player);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%coins%", String.valueOf(currentCoins));

        List<String> lore = plugin.getMessagesManager().getMessageList("coin-exchange.info-lore", placeholders);

        ItemStack infoItem = new ItemBuilder(coinProvider.getIcon())
                .setDisplayName(plugin.getMessagesManager().getMessage("coin-exchange.info-title"))
                .setLore(lore)
                .addGlow(true)
                .build();

        inventory.setItem(INFO_SLOT, infoItem);
    }

    private void setupInstructions() {
        ItemStack backButton = new ItemBuilder(Material.ARROW)
                .setDisplayName(plugin.getMessagesManager().getMessage("coin-exchange.back-to-shop"))
                .setLore(List.of(
                        "",
                        plugin.getMessagesManager().getMessage("coin-exchange.back-to-shop-lore")
                ))
                .build();

        inventory.setItem(BACK_BUTTON_SLOT, backButton);

        ItemStack instructionItem = new ItemBuilder(Material.BOOK)
                .setDisplayName(plugin.getMessagesManager().getMessage("coin-exchange.instructions-title"))
                .setLore(plugin.getMessagesManager().getMessageList("coin-exchange.instructions-lore"))
                .build();

        inventory.setItem(45, instructionItem);
    }

    private String getMaterialDisplayName(Material material) {
        return LanguageManager.translate(material);
    }

    public void open() {
        player.openInventory(inventory);
        playSound(Sound.BLOCK_CHEST_OPEN, 0.5f);

        BoosterItemListener listener = getBoosterItemListener();
        if (listener != null) {
            listener.registerCoinExchangeGUI(player, this);
        }
    }

    public void handleClick(int slot) {
        if (slot == BACK_BUTTON_SLOT) {
            new BoosterShopGUI(plugin, player).open();
            return;
        }

        boolean isExchangeSlot = false;
        for (int exchangeSlot : EXCHANGE_SLOTS) {
            if (slot == exchangeSlot) {
                isExchangeSlot = true;
                break;
            }
        }

        if (!isExchangeSlot) {
            return;
        }

        ItemStack clickedItem = inventory.getItem(slot);
        if (clickedItem == null || clickedItem.getType() == Material.BARRIER) return;

        Material material = clickedItem.getType();
        int requiredAmount = clickedItem.getAmount();

        processExchange(material, requiredAmount);
    }

    private void processExchange(Material material, int requiredAmount) {
        int playerAmount = countPlayerItems(material);

        if (playerAmount < requiredAmount) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%material%", getMaterialDisplayName(material));
            placeholders.put("%required%", String.valueOf(requiredAmount));
            placeholders.put("%current%", String.valueOf(playerAmount));

            plugin.getMessagesManager().sendMessage(player,
                    plugin.getMessagesManager().getMessage("coin-exchange.not-enough-items", placeholders));
            playSound(Sound.ENTITY_VILLAGER_NO, 1.0f);
            updateGUI();
            return;
        }

        if (requiredAmount >= 16) {
            new ConfirmExchangeGUI(plugin, player, material, requiredAmount, this).open();
            return;
        }

        executeExchange(material, requiredAmount);
    }

    public void executeExchange(Material material, int requiredAmount) {
        removeItemsFromPlayer(material, requiredAmount);
        coinProvider.deposit(player, 1);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%material%", getMaterialDisplayName(material));
        placeholders.put("%amount%", String.valueOf(requiredAmount));

        plugin.getMessagesManager().sendMessage(player,
                plugin.getMessagesManager().getMessage("coin-exchange.exchange-success", placeholders));
        playSound(Sound.ENTITY_PLAYER_LEVELUP, 1.0f);

        updateGUI();
    }

    private void updateGUI() {
        setupExchangeItems();
        setupInfoItem();
    }

    private int countPlayerItems(Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeItemsFromPlayer(Material material, int amountToRemove) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                int itemAmount = item.getAmount();
                if (itemAmount <= amountToRemove) {
                    amountToRemove -= itemAmount;
                    player.getInventory().removeItem(item);
                } else {
                    item.setAmount(itemAmount - amountToRemove);
                    amountToRemove = 0;
                }

                if (amountToRemove <= 0) break;
            }
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
