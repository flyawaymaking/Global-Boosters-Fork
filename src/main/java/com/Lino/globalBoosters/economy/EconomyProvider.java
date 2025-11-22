package com.Lino.globalBoosters.economy;

import org.bukkit.Material;
import org.bukkit.entity.Player;

public interface EconomyProvider {

    boolean isAvailable();

    double getBalance(Player player);

    boolean hasEnough(Player player, double amount);

    boolean withdraw(Player player, double amount);

    boolean deposit(Player player, double amount);

    String format(double amount);

    Double getPriceMultiplier();

    String getCurrencyName();

    Material getIcon();
}
