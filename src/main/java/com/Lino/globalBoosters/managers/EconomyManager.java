package com.Lino.globalBoosters.managers;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.config.ConfigManager;
import com.Lino.globalBoosters.economy.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EconomyManager {

    private final GlobalBoosters plugin;
    private final Map<String, EconomyProvider> providers = new HashMap<>();
    private String defaultProvider;

    public EconomyManager(GlobalBoosters plugin) {
        this.plugin = plugin;
        setupProviders();
    }

    private void setupProviders() {
        providers.clear();
        defaultProvider = null;
        ConfigManager configManager = plugin.getConfigManager();
        if (configManager.isBoosterCoinsEnabled()) {
            BoosterCoinProvider boosterCoinProvider = new BoosterCoinProvider(plugin, configManager.getBoosterCoinsName(),
                    configManager.getBoosterCoinsMaterialName(), configManager.getBoosterCoinsMultiplier(), configManager.getBoosterCoinsExchangeRates());
            if (boosterCoinProvider.isAvailable()) {
                providers.put("boostercoins", boosterCoinProvider);
                if (defaultProvider == null) defaultProvider = "boostercoins";
            }
        }
        if (configManager.isVaultEnabled()) {
            VaultProvider vaultProvider = new VaultProvider(plugin, configManager.getVaultItem(), configManager.getVaultMultiplier());
            if (vaultProvider.isAvailable()) {
                providers.put("vault", vaultProvider);
                if (defaultProvider == null) defaultProvider = "vault";
            }
        }
        if (configManager.isCoinsEngineEnabled()) {
            for (Map.Entry<String, Double> entry : configManager.getCoinsEngineCurrencies().entrySet()) {
                String currencyId = entry.getKey();
                Double multiplier = entry.getValue();

                CoinsEngineProvider coinsEngineProvider = new CoinsEngineProvider(plugin, currencyId, multiplier);
                if (coinsEngineProvider.isAvailable()) {
                    providers.put("coinsengine-" + currencyId, coinsEngineProvider);
                    if (defaultProvider == null) defaultProvider = "coinsengine-" + currencyId;
                }
            }
        }
    }

    public String getNextProvider(String currentProvider) {
        List<String> availableProviders = new ArrayList<>(providers.keySet());
        if (availableProviders.isEmpty()) return null;

        int currentIndex = availableProviders.indexOf(currentProvider);
        int nextIndex = (currentIndex + 1) % availableProviders.size();
        return availableProviders.get(nextIndex);
    }

    public EconomyProvider getDefaultProvider() {
        return providers.get(defaultProvider);
    }

    public String getDefaultProviderKey() {
        return defaultProvider;
    }


    public EconomyProvider getProvider(String key) {
        return providers.get(key);
    }

    public void reload() {
        setupProviders();
    }

    public boolean isAvailable() {
        return !providers.isEmpty();
    }
}
