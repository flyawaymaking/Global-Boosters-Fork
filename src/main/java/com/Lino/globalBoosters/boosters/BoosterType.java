package com.Lino.globalBoosters.boosters;

import org.bukkit.Material;

public enum BoosterType {
    PLANT_GROWTH(Material.WHEAT, 20.0, false),
    SPAWNER_RATE(Material.SPAWNER, 2.0, false),
    EXP_MULTIPLIER(Material.EXPERIENCE_BOTTLE, 2.0, false),
    MOB_DROP(Material.ROTTEN_FLESH, 2.0, false),
    MINING_SPEED(Material.DIAMOND_PICKAXE, 1.5, false),
    FISHING_LUCK(Material.FISHING_ROD, 2.0, false),
    FARMING_FORTUNE(Material.GOLDEN_HOE, 2.0, false),
    COMBAT_DAMAGE(Material.DIAMOND_SWORD, 1.5, false),
    HASTE(Material.GOLDEN_PICKAXE, 1.0, true),
    RESISTANCE(Material.SHIELD, 1.0, true),
    JUMP_BOOST(Material.RABBIT_FOOT, 1.0, true),
    REGENERATION(Material.GOLDEN_APPLE, 1.0, true),
    NIGHT_VISION(Material.GOLDEN_CARROT, 1.0, true),
    FIRE_RESISTANCE(Material.MAGMA_CREAM, 1.0, true),
    SPEED(Material.SUGAR, 1.0, true),
    STRENGTH(Material.BLAZE_POWDER, 1.0, true),
    NO_FALL_DAMAGE(Material.FEATHER, 1.0, false),
    HUNGER_SAVER(Material.COOKED_BEEF, 0.5, false),
    ARMOR_DURABILITY(Material.ANVIL, 0.5, false),
    KEEP_INVENTORY(Material.ENDER_CHEST, 1.0, false),
    FLY(Material.ELYTRA, 1.0, false),
    SLOWNESS(Material.SOUL_SAND, 1.0, true),
    MINING_FATIGUE(Material.PRISMARINE_SHARD, 1.0, true),
    WEAKNESS(Material.FERMENTED_SPIDER_EYE, 1.0, true),
    POISON(Material.SPIDER_EYE, 1.0, true),
    WITHER(Material.WITHER_SKELETON_SKULL, 1.0, true),
    BLINDNESS(Material.INK_SAC, 1.0, true),
    HUNGER(Material.ROTTEN_FLESH, 1.0, true),
    NAUSEA(Material.PUFFERFISH, 1.0, true);

    private final Material icon;
    private final double defaultMultiplier;
    private final boolean isEffectBooster;

    BoosterType(Material icon, double defaultMultiplier, boolean isEffectBooster) {
        this.icon = icon;
        this.defaultMultiplier = defaultMultiplier;
        this.isEffectBooster = isEffectBooster;
    }

    public String getDisplayName() {
        return name().replace("_", " ").toLowerCase().replace("exp", "experience");
    }

    public Material getIcon() {
        return icon;
    }

    public double getDefaultMultiplier() {
        return defaultMultiplier;
    }

    public boolean isEffectBooster() {
        return isEffectBooster;
    }

    public boolean isNegativeEffect() {
        return switch (this) {
            case SLOWNESS, MINING_FATIGUE, WEAKNESS, POISON, WITHER, BLINDNESS, HUNGER, NAUSEA -> true;
            default -> false;
        };
    }
}
