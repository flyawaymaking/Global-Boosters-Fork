package com.Lino.globalBoosters.managers;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.economy.*;
import org.bukkit.entity.Player;

public class EconomyManager {

    private final GlobalBoosters plugin;
    private EconomyProvider provider;

    public EconomyManager(GlobalBoosters plugin) {
        this.plugin = plugin;
        setupProviders();
    }

    private void setupProviders() {
        if (plugin.getConfigManager().isCoinsEngineEnabled()) {
            CoinsEngineEconomyProvider coinsProvider = new CoinsEngineEconomyProvider(plugin);
            if (coinsProvider.isAvailable()) {
                provider = coinsProvider;
                return;
            }
        }
        VaultEconomyProvider vaultProvider = new VaultEconomyProvider(plugin);
        if (vaultProvider.isAvailable()) {
            provider = vaultProvider;
            return;
        }
        provider = null;
    }

    public EconomyProvider getProvider() {
        return provider;
    }

    public void reload() {
        setupProviders();
    }

    public double getBalance(Player player) {
        return provider.getBalance(player);
    }

    public boolean hasEnough(Player player, double amount) {
        return provider.hasEnough(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        return provider.withdraw(player, amount);
    }

    public boolean deposit(Player player, double amount) {
        return provider.deposit(player, amount);
    }

    public String format(double amount) {
        return provider.format(amount);
    }
}
