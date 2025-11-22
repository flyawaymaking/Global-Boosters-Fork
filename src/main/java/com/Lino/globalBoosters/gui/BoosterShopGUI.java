package com.Lino.globalBoosters.gui;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.BoosterType;
import com.Lino.globalBoosters.economy.EconomyProvider;
import com.Lino.globalBoosters.listeners.BoosterItemListener;
import com.Lino.globalBoosters.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BoosterShopGUI {

    private final GlobalBoosters plugin;
    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, BoosterType> slotToBooster;
    private String currentProviderKey;

    private static final int[] BOOSTER_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43,
            46, 47, 48, 49, 50, 51, 52
    };

    private static final int[] DECORATION_SLOTS = {
            1, 2, 3, 4, 5, 6, 7,
            9, 17, 18, 26, 27, 35, 36, 44, 45, 53
    };

    private static int CURRENCY_CHANGE_SLOT = 0;
    private static int BOOSTER_COINS_EXCHANGE_SLOT = 8;

    public BoosterShopGUI(GlobalBoosters plugin, Player player) {
        this(plugin, player, plugin.getEconomyManager().getDefaultProviderKey());
    }

    public BoosterShopGUI(GlobalBoosters plugin, Player player, String providerKey) {
        this.plugin = plugin;
        this.player = player;
        this.currentProviderKey = providerKey != null ? providerKey : plugin.getEconomyManager().getDefaultProviderKey();
        this.slotToBooster = new HashMap<>();

        this.inventory = Bukkit.createInventory(null, 54, plugin.getMessagesManager().getMessageComponent("shop.title", null));
        setupGUI();
    }

    private void setupGUI() {
        fillDecoration();
        List<BoosterType> availableBoosters = new ArrayList<>();

        for (BoosterType type : BoosterType.values()) {
            if (plugin.getConfigManager().isBoosterEnabled(type)) {
                String boosterPermission = "globalboosters.use." + type.name().toLowerCase();
                if (player.hasPermission(boosterPermission) || player.hasPermission("globalboosters.use.*")) {
                    availableBoosters.add(type);
                }
            }
        }

        int slotIndex = 0;
        for (int i = 0; i < availableBoosters.size() && slotIndex < BOOSTER_SLOTS.length; i++) {
            BoosterType type = availableBoosters.get(i);
            int slot = BOOSTER_SLOTS[slotIndex];
            inventory.setItem(slot, createBoosterItem(type));
            slotToBooster.put(slot, type);
            slotIndex++;
        }
        setupNavigationButtons();
    }

    private void setupNavigationButtons() {
        if (plugin.getEconomyManager().getProvider("boostercoins") != null &&
                plugin.getEconomyManager().getProvider("boostercoins").isAvailable()) {

            ItemStack coinExchangeButton = new ItemBuilder(Material.TARGET)
                    .setDisplayName(plugin.getMessagesManager().getMessage("shop.coin-exchange-button"))
                    .setLore(plugin.getMessagesManager().getMessageList("shop.coin-exchange-button-lore"))
                    .build();

            inventory.setItem(BOOSTER_COINS_EXCHANGE_SLOT, coinExchangeButton);
        }
        setupCurrencyChangeButton();
    }

    private void setupCurrencyChangeButton() {
        EconomyProvider currentProvider = getCurrentProvider();
        if (currentProvider == null) return;

        double balance = currentProvider.getBalance(player);
        String nextProviderKey = plugin.getEconomyManager().getNextProvider(currentProviderKey);
        EconomyProvider nextProvider = plugin.getEconomyManager().getProvider(nextProviderKey);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%currency%", currentProvider.getCurrencyName());
        placeholders.put("%balance%", currentProvider.format(balance));
        placeholders.put("%next_currency%", nextProviderKey != null ? nextProvider.getCurrencyName() : "N/A");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(plugin.getMessagesManager().getMessage("shop.current-balance", placeholders));
        if (nextProviderKey != null) {
            lore.add(plugin.getMessagesManager().getMessage("shop.next-currency", placeholders));
        }
        lore.add("");
        lore.add(plugin.getMessagesManager().getMessage("shop.click-to-change"));

        Material icon = currentProvider.getIcon();

        ItemStack currencyButton = new ItemBuilder(icon)
                .setDisplayName(plugin.getMessagesManager().getMessage("shop.current-currency", placeholders))
                .setLore(lore)
                .build();

        inventory.setItem(CURRENCY_CHANGE_SLOT, currencyButton);
    }

    private void fillDecoration() {
        ItemStack decorationItem = new ItemBuilder(Material.YELLOW_STAINED_GLASS_PANE)
                .setDisplayName(" ")
                .build();
        for (int slot : DECORATION_SLOTS) {
            inventory.setItem(slot, decorationItem);
        }
    }

    private ItemStack createBoosterItem(BoosterType type) {
        EconomyProvider provider = getCurrentProvider();
        if (provider == null) {
            provider = plugin.getEconomyManager().getDefaultProvider();
        }

        double price = Math.ceil(plugin.getConfigManager().getBoosterPrice(type) * provider.getPriceMultiplier());
        int duration = plugin.getConfigManager().getBoosterDuration(type);
        boolean isActive = plugin.getBoosterManager().isBoosterActive(type);
        boolean limitedSupply = plugin.getConfigManager().isLimitedSupplyEnabled();
        int remaining = plugin.getSupplyManager().getRemainingPurchases(type);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%duration%", String.valueOf(duration));
        placeholders.put("%price%", provider.format(price));

        List<String> lore = new ArrayList<>();
        lore.add("");
        if (!isNoMultiplierBooster(type) && (!type.isEffectBooster() || type == BoosterType.PLANT_GROWTH)) {
            double multiplier = plugin.getConfigManager().getBoosterMultiplier(type);
            placeholders.put("%multiplier%", String.valueOf(multiplier));
            lore.add(plugin.getMessagesManager().getMessage("shop.item-lore.multiplier", placeholders));
        }

        lore.add(plugin.getMessagesManager().getMessage("shop.item-lore.duration", placeholders));
        lore.add(plugin.getMessagesManager().getMessage("shop.item-lore.price", placeholders));
        if (limitedSupply) {
            lore.add("");
            if (remaining > 0) {
                placeholders.put("%remaining%", String.valueOf(remaining));
                lore.add(plugin.getMessagesManager().getMessage("shop.item-lore.supply-remaining", placeholders));
            } else {
                lore.add(plugin.getMessagesManager().getMessage("shop.item-lore.out-of-stock"));
            }
            String resetTime = plugin.getSupplyManager().getTimeUntilReset(type);
            placeholders.put("%time%", resetTime);
            lore.add(plugin.getMessagesManager().getMessage("shop.item-lore.next-restock", placeholders));
        }

        lore.add("");
        if (isActive) {
            lore.add(plugin.getMessagesManager().getMessage("shop.item-lore.already-active"));
        }

        if (limitedSupply && remaining <= 0) {
            lore.add(plugin.getMessagesManager().getMessage("shop.item-lore.cannot-purchase"));
        } else {
            lore.add(plugin.getMessagesManager().getMessage("shop.item-lore.click-to-purchase"));
        }

        Material icon = type.getIcon();
        if (limitedSupply && remaining <= 0) {
            icon = Material.BARRIER;
        }

        return new ItemBuilder(icon)
                .setDisplayName(plugin.getMessagesManager().getBoosterName(type))
                .setLore(lore)
                .addGlow(true)
                .build();
    }

    private boolean isNoMultiplierBooster(BoosterType type) {
        return switch (type) {
            case NO_FALL_DAMAGE, KEEP_INVENTORY, FLY -> true;
            default -> false;
        };
    }

    public void open() {
        player.openInventory(inventory);
        playSound(Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);

        BoosterItemListener listener = getBoosterItemListener();
        if (listener != null) {
            listener.registerShopGUI(player, this);
        }
    }

    public void handleClick(int slot) {
        if (slot == BOOSTER_COINS_EXCHANGE_SLOT) {
            new CoinExchangeGUI(plugin, player).open();
            return;
        }

        if (slot == CURRENCY_CHANGE_SLOT) {
            String nextProviderKey = plugin.getEconomyManager().getNextProvider(currentProviderKey);
            if (nextProviderKey != null) {
                currentProviderKey = nextProviderKey;
                new BoosterShopGUI(plugin, player, currentProviderKey).open();
                playSound(Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            }
            return;
        }

        BoosterType type = slotToBooster.get(slot);
        if (type == null) {
            return;
        }

        if (plugin.getConfigManager().isLimitedSupplyEnabled()) {
            if (!plugin.getSupplyManager().canPurchase(type)) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(type));
                plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("purchase.out-of-stock", placeholders));
                playSound(Sound.ENTITY_VILLAGER_NO, 1f, 1.0f);
                return;
            }
        }

        EconomyProvider provider = getCurrentProvider();
        if (provider == null) {
            plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("purchase.economy-unavailable"));
            return;
        }

        double price = Math.ceil(plugin.getConfigManager().getBoosterPrice(type) * provider.getPriceMultiplier());

        if (!provider.hasEnough(player, price)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%price%", provider.format(price));
            plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("purchase.not-enough-money", placeholders));
            playSound(Sound.ENTITY_VILLAGER_NO, 1f, 1.0f);
            return;
        }

        new ConfirmPurchaseGUI(plugin, player, type, price, currentProviderKey).open();
    }

    public EconomyProvider getCurrentProvider() {
        return plugin.getEconomyManager().getProvider(currentProviderKey);
    }

    public Inventory getInventory() {
        return inventory;
    }

    private void playSound(Sound sound, float volumeMultiplier, float pitch) {
        float volume = (float) plugin.getConfigManager().getSoundVolume() * volumeMultiplier;
        if (volume > 0) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    private BoosterItemListener getBoosterItemListener() {
        return plugin.getBoosterItemListener();
    }
}
