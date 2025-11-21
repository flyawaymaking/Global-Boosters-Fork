package com.Lino.globalBoosters.items;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.BoosterType;
import com.Lino.globalBoosters.utils.ItemBuilder;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class BoosterItem {

    public static final NamespacedKey BOOSTER_TYPE_KEY = new NamespacedKey("globalboosters", "booster_type");
    public static final NamespacedKey BOOSTER_DURATION_KEY = new NamespacedKey("globalboosters", "booster_duration");

    public static ItemStack createBoosterItem(BoosterType type, int durationMinutes) {
        GlobalBoosters plugin = GlobalBoosters.getInstance();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(type));
        placeholders.put("%duration%", String.valueOf(durationMinutes));

        List<String> lore = new ArrayList<>(Arrays.asList(
                plugin.getMessagesManager().getMessage("booster.item-lore.line1"),
                plugin.getMessagesManager().getMessage("booster.item-lore.line2"),
                plugin.getMessagesManager().getMessage("booster.item-lore.line3"),
                plugin.getMessagesManager().getMessage("booster.item-lore.line4")
        ));

        if (!isNoMultiplierBooster(type) && (!type.isEffectBooster() || type == BoosterType.PLANT_GROWTH)) {
            placeholders.put("%multiplier%", String.valueOf(plugin.getConfigManager().getBoosterMultiplier(type)));
            lore.add(plugin.getMessagesManager().getMessage("booster.item-lore.multiplier", placeholders));
        }

        lore.addAll(Arrays.asList(
                plugin.getMessagesManager().getMessage("booster.item-lore.duration", placeholders),
                plugin.getMessagesManager().getMessage("booster.item-lore.line5"),
                plugin.getMessagesManager().getMessage("booster.item-lore.effects"),
                getEffectDescription(type),
                "",
                plugin.getMessagesManager().getMessage("booster.item-lore.activate")
        ));

        ItemStack item = new ItemBuilder(type.getIcon())
                .setDisplayName(plugin.getMessagesManager().getMessage("booster.item-name", placeholders))
                .setLore(lore)
                .addGlow(true)
                .build();

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(BOOSTER_TYPE_KEY, PersistentDataType.STRING, type.name());
        container.set(BOOSTER_DURATION_KEY, PersistentDataType.INTEGER, durationMinutes);
        item.setItemMeta(meta);

        return item;
    }

    private static boolean isNoMultiplierBooster(BoosterType type) {
        return switch (type) {
            case NO_FALL_DAMAGE, KEEP_INVENTORY, FLY -> true;
            default -> false;
        };
    }

    public static boolean isBoosterItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(BOOSTER_TYPE_KEY, PersistentDataType.STRING);
    }

    public static BoosterType getBoosterType(ItemStack item) {
        if (!isBoosterItem(item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String typeName = container.get(BOOSTER_TYPE_KEY, PersistentDataType.STRING);

        try {
            return BoosterType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static int getBoosterDuration(ItemStack item) {
        if (!isBoosterItem(item)) {
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.getOrDefault(BOOSTER_DURATION_KEY, PersistentDataType.INTEGER, 30);
    }

    private static String getEffectDescription(BoosterType type) {
        GlobalBoosters plugin = GlobalBoosters.getInstance();
        return plugin.getMessagesManager().getMessage("effects." + type.name().toLowerCase());
    }
}
