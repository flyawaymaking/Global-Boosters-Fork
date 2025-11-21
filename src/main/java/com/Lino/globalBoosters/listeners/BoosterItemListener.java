package com.Lino.globalBoosters.listeners;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.BoosterType;
import com.Lino.globalBoosters.gui.BoosterShopGUI;
import com.Lino.globalBoosters.gui.ConfirmPurchaseGUI;
import com.Lino.globalBoosters.items.BoosterItem;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class BoosterItemListener implements Listener {

    private final GlobalBoosters plugin;
    private final Map<Player, BoosterShopGUI> shopGUIs;
    private final Map<Player, ConfirmPurchaseGUI> confirmGUIs;

    public BoosterItemListener(GlobalBoosters plugin) {
        this.plugin = plugin;
        this.shopGUIs = new HashMap<>();
        this.confirmGUIs = new HashMap<>();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK &&
                event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!BoosterItem.isBoosterItem(item)) {
            return;
        }

        event.setCancelled(true);

        BoosterType type = BoosterItem.getBoosterType(item);
        int duration = BoosterItem.getBoosterDuration(item);
        if (type == null) {
            return;
        }

        float volume = (float) plugin.getConfigManager().getSoundVolume();

        if (!plugin.getConfigManager().isBoosterEnabled(type)) {
            plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("booster.disabled"));
            if (volume > 0) player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, volume, 1.0f);
            return;
        }

        if (!player.hasPermission("globalboosters.use")) {
            plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("general.no-permission-use"));
            return;
        }

        String boosterPermission = "globalboosters.use." + type.name().toLowerCase();
        if (!player.hasPermission(boosterPermission) && !player.hasPermission("globalboosters.use.*")) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%booster%", plugin.getMessagesManager().getBoosterNameRaw(type));
            plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("general.no-permission-booster", placeholders));
            if (volume > 0) player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, volume, 1.0f);
            return;
        }

        if (plugin.getBoosterManager().isBoosterActive(type)) {
            plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("booster.already-active"));
            if (volume > 0) player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, volume, 1.0f);
            return;
        }

        int maxActive = plugin.getConfigManager().getMaxActiveBoosters();
        if (maxActive != -1 && plugin.getBoosterManager().getActiveBoosterCount() >= maxActive) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%max%", String.valueOf(maxActive));
            plugin.getMessagesManager().sendMessage(player, plugin.getMessagesManager().getMessage("booster.max-active-reached", placeholders));
            if (volume > 0) player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, volume, 1.0f);
            return;
        }

        if (plugin.getBoosterManager().activateBooster(type, player, duration)) {
            item.setAmount(item.getAmount() - 1);
            if (volume > 0) player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, volume, 1.0f);
        } else {
            if (volume > 0) player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, volume, 1.0f);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        if (BoosterItem.isBoosterItem(clickedItem)) {
            if (event.getSlotType() == InventoryType.SlotType.ARMOR ||
                    event.getSlotType() == InventoryType.SlotType.QUICKBAR && event.getSlot() == 40) {
                event.setCancelled(true);
                return;
            }
        }

        if (BoosterItem.isBoosterItem(cursorItem)) {
            if (event.getSlotType() == InventoryType.SlotType.ARMOR ||
                    event.getSlotType() == InventoryType.SlotType.QUICKBAR && event.getSlot() == 40) {
                event.setCancelled(true);
                return;
            }

            InventoryType invType = event.getInventory().getType();
            if (invType == InventoryType.CRAFTING || invType == InventoryType.WORKBENCH ||
                    invType == InventoryType.ANVIL || invType == InventoryType.ENCHANTING ||
                    invType == InventoryType.SMITHING || invType == InventoryType.GRINDSTONE ||
                    invType == InventoryType.STONECUTTER || invType == InventoryType.LOOM ||
                    invType == InventoryType.CARTOGRAPHY || invType == InventoryType.BREWING ||
                    invType == InventoryType.FURNACE || invType == InventoryType.BLAST_FURNACE ||
                    invType == InventoryType.SMOKER) {
                if (event.getRawSlot() < event.getInventory().getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (event.isShiftClick() && BoosterItem.isBoosterItem(clickedItem)) {
            InventoryType topType = player.getOpenInventory().getTopInventory().getType();
            if (topType != InventoryType.PLAYER && topType != InventoryType.CREATIVE &&
                    topType != InventoryType.CHEST && topType != InventoryType.ENDER_CHEST &&
                    topType != InventoryType.SHULKER_BOX && topType != InventoryType.BARREL) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getInventory().getHolder() != null) {
            return;
        }

        if (shopGUIs.containsKey(player)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            if (event.getRawSlot() < event.getInventory().getSize()) {
                BoosterShopGUI gui = shopGUIs.get(player);
                gui.handleClick(event.getRawSlot());
            }
        } else if (confirmGUIs.containsKey(player)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            if (event.getRawSlot() < event.getInventory().getSize()) {
                ConfirmPurchaseGUI gui = confirmGUIs.get(player);
                gui.handleClick(event.getRawSlot());
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            shopGUIs.remove(player);
            confirmGUIs.remove(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getBossBarManager().addPlayerToBossBars(event.getPlayer());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (!BoosterItem.isBoosterItem(event.getOldCursor())) {
            return;
        }

        InventoryType invType = event.getInventory().getType();
        if (invType == InventoryType.CRAFTING || invType == InventoryType.WORKBENCH ||
                invType == InventoryType.ANVIL || invType == InventoryType.ENCHANTING ||
                invType == InventoryType.SMITHING || invType == InventoryType.GRINDSTONE ||
                invType == InventoryType.STONECUTTER || invType == InventoryType.LOOM ||
                invType == InventoryType.CARTOGRAPHY || invType == InventoryType.BREWING ||
                invType == InventoryType.FURNACE || invType == InventoryType.BLAST_FURNACE ||
                invType == InventoryType.SMOKER) {

            for (int slot : event.getRawSlots()) {
                if (slot < event.getInventory().getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        for (int slot : event.getRawSlots()) {
            if (slot >= 5 && slot <= 8) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (BoosterItem.isBoosterItem(event.getMainHandItem()) ||
                BoosterItem.isBoosterItem(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (BoosterItem.isBoosterItem(event.getPlayerItem())) {
            event.setCancelled(true);
        }
    }

    public void registerShopGUI(Player player, BoosterShopGUI gui) {
        shopGUIs.put(player, gui);
    }

    public void registerConfirmGUI(Player player, ConfirmPurchaseGUI gui) {
        confirmGUIs.put(player, gui);
    }

    public void unregisterGUIs(Player player) {
        shopGUIs.remove(player);
        confirmGUIs.remove(player);
    }
}
