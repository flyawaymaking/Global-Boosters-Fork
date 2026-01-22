package com.Lino.globalBoosters.economy;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class BoosterCoinProvider implements EconomyProvider {

    private final GlobalBoosters plugin;
    private final String currencyName;
    private final Double multiplier;
    private final String materialName;
    private final Map<String, Integer> exchangeRates;
    private final NamespacedKey coinKey;
    private Material icon = Material.GOLD_NUGGET;

    public BoosterCoinProvider(GlobalBoosters plugin, @NotNull String currencyName, @NotNull String materialName,
                               @NotNull Double multiplier, @NotNull Map<String, Integer> exchangeRates) {
        this.plugin = plugin;
        this.currencyName = currencyName;
        this.multiplier = multiplier;
        this.materialName = materialName;
        this.coinKey = new NamespacedKey(plugin, "booster_coin");
        this.exchangeRates = exchangeRates;
        loadConfig();
    }

    private void loadConfig() {
        Material newMaterial = Material.getMaterial(materialName.toUpperCase());
        if (newMaterial == null) {
            plugin.getLogger().warning("Invalid booster coin material: " + materialName + ", using GOLD_NUGGET instead");
        } else {
            icon = newMaterial;
        }
    }

    public ItemStack createBoosterCoin(int amount) {
        ItemBuilder builder = new ItemBuilder(icon).setAmount(amount).setDisplayName(currencyName)
                .setLore(plugin.getMessagesManager().getMessageList("coins.lore")).addGlow(true);

        ItemStack coin = builder.build();

        ItemMeta meta = coin.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(coinKey, PersistentDataType.BYTE, (byte) 1);
            coin.setItemMeta(meta);
        }

        return coin;
    }

    public boolean isBoosterCoin(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(coinKey, PersistentDataType.BYTE);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public double getBalance(Player player) {
        int totalCoins = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (isBoosterCoin(item)) {
                totalCoins += item.getAmount();
            }
        }

        return totalCoins;
    }

    @Override
    public boolean hasEnough(Player player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        int coinsToRemove = (int) amount;
        if (!hasEnough(player, coinsToRemove)) {
            return false;
        }

        int remainingToRemove = coinsToRemove;

        for (int i = 0; i < player.getInventory().getSize() && remainingToRemove > 0; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isBoosterCoin(item)) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remainingToRemove) {
                    player.getInventory().setItem(i, null);
                    remainingToRemove -= itemAmount;
                } else {
                    item.setAmount(itemAmount - remainingToRemove);
                    remainingToRemove = 0;
                }
            }
        }

        return true;
    }

    @Override
    public boolean deposit(Player player, double amount) {
        int coinsToAdd = (int) amount;
        if (coinsToAdd <= 0) {
            return false;
        }

        ItemStack coins = createBoosterCoin(coinsToAdd);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(coins);

        if (!leftover.isEmpty()) {
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), item);
            }
        }

        return true;
    }

    @Override
    public String format(double amount) {
        int coins = (int) amount;
        Map<String, String> placeholders = Map.of("%amount%", String.valueOf(coins));
        return plugin.getMessagesManager().getMessage("coins.format", placeholders);
    }

    public Map<String, Integer> getExchangeRates() {
        return new HashMap<>(exchangeRates);
    }

    @Override
    public Double getPriceMultiplier() {
        return multiplier;
    }

    @Override
    public String getCurrencyName() {
        return currencyName;
    }

    @Override
    public Material getIcon() {
        return icon;
    }
}
