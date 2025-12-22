package com.murilloskills.utils;

import com.murilloskills.models.SkillReceptorResult;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

/**
 * Utility class for calculating Blacksmith XP based on different actions.
 */
public class BlacksmithXpGetter {

    // Base XP values for anvil actions
    // Note: Constants replaced by SkillConfig getters

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
            xp += SkillConfig.getBlacksmithXpAnvilRepair();
        }
        if (isEnchantCombine) {
            xp += SkillConfig.getBlacksmithXpAnvilEnchantCombine();
        }
        if (isRename && !isRepair && !isEnchantCombine) {
            xp += SkillConfig.getBlacksmithXpAnvilRename();
        }

        return Math.max(xp, SkillConfig.getBlacksmithXpAnvilRename()); // Minimum XP for any anvil action
    }

    /**
     * Get XP for enchanting based on the enchantment level slot (0, 1, 2).
     * 
     * @param slot The enchantment slot (0 = level 1, 1 = level 2, 2 = level 3)
     * @return XP amount
     */
    public static int getEnchantXp(int slot) {
        return switch (slot) {
            case 0 -> SkillConfig.getBlacksmithXpEnchantLevel1();
            case 1 -> SkillConfig.getBlacksmithXpEnchantLevel2();
            case 2 -> SkillConfig.getBlacksmithXpEnchantLevel3();
            default -> SkillConfig.getBlacksmithXpEnchantLevel1();
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
            return new SkillReceptorResult(true, SkillConfig.getBlacksmithXpSmeltIron());
        }
        if (inputItem == Items.RAW_GOLD || inputItem == Items.GOLD_ORE || inputItem == Items.DEEPSLATE_GOLD_ORE
                || inputItem == Items.NETHER_GOLD_ORE) {
            return new SkillReceptorResult(true, SkillConfig.getBlacksmithXpSmeltGold());
        }
        if (inputItem == Items.RAW_COPPER || inputItem == Items.COPPER_ORE || inputItem == Items.DEEPSLATE_COPPER_ORE) {
            return new SkillReceptorResult(true, SkillConfig.getBlacksmithXpSmeltCopper());
        }
        if (inputItem == Items.ANCIENT_DEBRIS) {
            return new SkillReceptorResult(true, SkillConfig.getBlacksmithXpSmeltAncientDebris());
        }

        return new SkillReceptorResult(false, 0);
    }

    /**
     * Get XP for using the grindstone.
     * 
     * @return XP amount
     */
    public static int getGrindstoneXp() {
        return SkillConfig.getBlacksmithXpGrindstoneUse();
    }
}
