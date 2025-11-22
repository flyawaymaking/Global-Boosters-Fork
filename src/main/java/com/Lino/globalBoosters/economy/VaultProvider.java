package com.Lino.globalBoosters.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class VaultProvider implements EconomyProvider {

    private final JavaPlugin plugin;
    private final String materialName;
    private final Double multiplier;
    private Economy economy;
    private String currencyName = "$";
    private Material icon = Material.KELP;

    public VaultProvider(JavaPlugin plugin, @NotNull String materialName, @NotNull Double multiplier) {
        this.plugin = plugin;
        this.materialName = materialName;
        this.multiplier = multiplier;
        setupVault();
    }

    public void setupVault() {
        economy = null;
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        economy = rsp.getProvider();
        currencyName = economy.currencyNameSingular();

        Material newMaterial = Material.getMaterial(materialName.toUpperCase());
        if (newMaterial == null) {
            plugin.getLogger().warning("Invalid vault material: " + materialName + ", using KELP instead");
        } else {
            icon = newMaterial;
        }
    }

    @Override
    public boolean isAvailable() {
        return economy != null;
    }

    @Override
    public double getBalance(Player player) {
        return economy.getBalance(player);
    }

    @Override
    public boolean hasEnough(Player player, double amount) {
        return economy.has(player, amount);
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean deposit(Player player, double amount) {
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    @Override
    public String format(double amount) {
        return economy.format(amount);
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
