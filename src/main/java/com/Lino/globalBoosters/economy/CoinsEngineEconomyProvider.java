package com.Lino.globalBoosters.economy;

import com.Lino.globalBoosters.GlobalBoosters;
import org.bukkit.entity.Player;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;

public class CoinsEngineEconomyProvider implements EconomyProvider {

    private final GlobalBoosters plugin;
    private Currency currency;

    public CoinsEngineEconomyProvider(GlobalBoosters plugin) {
        this.plugin = plugin;
        setupCoinsEngine();
    }

    private void setupCoinsEngine() {
        String currencyName = plugin.getConfigManager().getCoinsEngineCurrency();
        try {
            currency = CoinsEngineAPI.getCurrency(currencyName);

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
}
