package com.Lino.globalBoosters.economy;

import com.Lino.globalBoosters.GlobalBoosters;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;

public class CoinsEngineProvider implements EconomyProvider {

    private final GlobalBoosters plugin;
    private final String currencyId;
    private final Double multiplier;
    private Currency currency;
    private String currencyName = "money";
    private Material icon = Material.SUNFLOWER;

    public CoinsEngineProvider(GlobalBoosters plugin, @NotNull String currencyId, @NotNull Double multiplier) {
        this.plugin = plugin;
        this.currencyId = currencyId;
        this.multiplier = multiplier;
        setupCoinsEngine();
    }

    private void setupCoinsEngine() {
        try {
            currency = CoinsEngineAPI.getCurrency(currencyId);
            assert currency != null;
            currencyName = currency.getName();
            icon = currency.icon().getMaterial();
        } catch (Exception e) {
            currency = null;
        }
    }

    @Override
    public boolean isAvailable() {
        return currency != null;
    }

    @Override
    public double getBalance(Player player) {
        if (!isAvailable()) return 0;

        try {
            return CoinsEngineAPI.getBalance(player, currency);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get balance from CoinsEngine: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public boolean hasEnough(Player player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        if (!isAvailable()) return false;

        try {
            CoinsEngineAPI.removeBalance(player, currency, amount);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to withdraw from CoinsEngine: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deposit(Player player, double amount) {
        if (!isAvailable()) return false;

        try {
            CoinsEngineAPI.addBalance(player, currency, amount);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deposit to CoinsEngine: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String format(double amount) {
        return currency.format(amount);
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
