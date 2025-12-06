package com.murilloskills.utils;

import com.murilloskills.models.SkillReceptorResult;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

/**
 * Utility class for calculating Blacksmith XP based on different actions.
 */
public class BlacksmithXpGetter {

    // Base XP values for anvil actions
    private static final int XP_ANVIL_REPAIR = 80;
    private static final int XP_ANVIL_RENAME = 50;
    private static final int XP_ANVIL_ENCHANT_COMBINE = 100;

    // XP for enchanting table
    private static final int XP_ENCHANT_LEVEL_1 = 40;
    private static final int XP_ENCHANT_LEVEL_2 = 70;
    private static final int XP_ENCHANT_LEVEL_3 = 100;

    // XP for smelting ores (only ores that produce ingots/materials)
    private static final int XP_SMELT_IRON = 15;
    private static final int XP_SMELT_GOLD = 25;
    private static final int XP_SMELT_COPPER = 12;
    private static final int XP_SMELT_ANCIENT_DEBRIS = 80;

    // XP for grindstone
    private static final int XP_GRINDSTONE_USE = 30;

    /**
     * Get XP for anvil use based on the action type.
     * 
     * @param isRepair         true if repairing an item
     * @param isEnchantCombine true if combining enchantments
     * @param isRename         true if just renaming
     * @return XP amount
     */
    public static int getAnvilXp(boolean isRepair, boolean isEnchantCombine, boolean isRename) {
        int xp = 0;

        if (isRepair) {
            xp += XP_ANVIL_REPAIR;
        }
        if (isEnchantCombine) {
            xp += XP_ANVIL_ENCHANT_COMBINE;
        }
        if (isRename && !isRepair && !isEnchantCombine) {
            xp += XP_ANVIL_RENAME;
        }

        return Math.max(xp, XP_ANVIL_RENAME); // Minimum XP for any anvil action
    }

    /**
     * Get XP for enchanting based on the enchantment level slot (0, 1, 2).
     * 
     * @param slot The enchantment slot (0 = level 1, 1 = level 2, 2 = level 3)
     * @return XP amount
     */
    public static int getEnchantXp(int slot) {
        return switch (slot) {
            case 0 -> XP_ENCHANT_LEVEL_1;
            case 1 -> XP_ENCHANT_LEVEL_2;
            case 2 -> XP_ENCHANT_LEVEL_3;
            default -> XP_ENCHANT_LEVEL_1;
        };
    }

    /**
     * Check if an item is a smeltable ore and return XP result.
     * Only ores that produce metal ingots/materials grant XP.
     * 
     * @param inputItem The item being smelted
     * @return SkillReceptorResult with XP if valid ore
     */
    public static SkillReceptorResult getSmeltingXp(Item inputItem) {
        if (inputItem == Items.RAW_IRON || inputItem == Items.IRON_ORE || inputItem == Items.DEEPSLATE_IRON_ORE) {
            return new SkillReceptorResult(true, XP_SMELT_IRON);
        }
        if (inputItem == Items.RAW_GOLD || inputItem == Items.GOLD_ORE || inputItem == Items.DEEPSLATE_GOLD_ORE
                || inputItem == Items.NETHER_GOLD_ORE) {
            return new SkillReceptorResult(true, XP_SMELT_GOLD);
        }
        if (inputItem == Items.RAW_COPPER || inputItem == Items.COPPER_ORE || inputItem == Items.DEEPSLATE_COPPER_ORE) {
            return new SkillReceptorResult(true, XP_SMELT_COPPER);
        }
        if (inputItem == Items.ANCIENT_DEBRIS) {
            return new SkillReceptorResult(true, XP_SMELT_ANCIENT_DEBRIS);
        }

        return new SkillReceptorResult(false, 0);
    }

    /**
     * Get XP for using the grindstone.
     * 
     * @return XP amount
     */
    public static int getGrindstoneXp() {
        return XP_GRINDSTONE_USE;
    }
}
