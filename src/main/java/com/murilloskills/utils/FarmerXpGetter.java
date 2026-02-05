package com.murilloskills.utils;

import com.murilloskills.models.SkillReceptorResult;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

/**
 * Utility class to determine XP rewards for Farmer skill actions.
 * 
 * XP Sources:
 * - Harvest mature crops: Base XP
 * - Plant seeds: +25% of base
 * - Compost items: +15% of base
 * - Create Bone Meal from Composter: +10% of base
 */
public class FarmerXpGetter {

    // Base XP values for different crops
    // Note: Constants replaced by SkillConfig getters

    /**
     * Checks if a block is a harvestable crop and returns the XP reward.
     * Only gives XP for mature crops.
     * 
     * @param block    The block to check
     * @param isMature Whether the crop is fully grown
     * @return SkillReceptorResult with XP info
     */
    public static SkillReceptorResult getCropHarvestXp(Block block, boolean isMature) {
        // Only give XP for mature crops
        if (!isMature) {
            return new SkillReceptorResult(false, 0);
        }

        // Wheat
        if (block == Blocks.WHEAT) {
            return new SkillReceptorResult(true, SkillConfig.getFarmerXpWheat());
        }
        // Carrots
        else if (block == Blocks.CARROTS) {
            return new SkillReceptorResult(true, SkillConfig.getFarmerXpCarrot());
        }
        // Potatoes
        else if (block == Blocks.POTATOES) {
            return new SkillReceptorResult(true, SkillConfig.getFarmerXpPotato());
        }
        // Beetroots
        else if (block == Blocks.BEETROOTS) {
            return new SkillReceptorResult(true, SkillConfig.getFarmerXpBeetroot());
        }
        // Melon
        else if (block == Blocks.MELON) {
            return new SkillReceptorResult(true, SkillConfig.getFarmerXpMelon());
        }
        // Pumpkin
        else if (block == Blocks.PUMPKIN) {
            return new SkillReceptorResult(true, SkillConfig.getFarmerXpPumpkin());
        }
        // Nether Wart
        else if (block == Blocks.NETHER_WART) {
            return new SkillReceptorResult(true, SkillConfig.getFarmerXpNetherWart());
        }
        // Sweet Berry Bush
        else if (block == Blocks.SWEET_BERRY_BUSH) {
            return new SkillReceptorResult(true, SkillConfig.getFarmerXpSweetBerry());
        }
        // Cocoa
        else if (block == Blocks.COCOA) {
            return new SkillReceptorResult(true, SkillConfig.getFarmerXpCocoa());
        }

        return new SkillReceptorResult(false, 0);
    }

    /**
     * Gets XP for planting seeds (25% of base harvest XP).
     * 
     * @param block The crop block being planted
     * @return SkillReceptorResult with XP info
     */
    public static SkillReceptorResult getPlantingXp(Block block) {
        int baseXp = 0;

        if (block == Blocks.WHEAT)
            baseXp = SkillConfig.getFarmerXpWheat();
        else if (block == Blocks.CARROTS)
            baseXp = SkillConfig.getFarmerXpCarrot();
        else if (block == Blocks.POTATOES)
            baseXp = SkillConfig.getFarmerXpPotato();
        else if (block == Blocks.BEETROOTS)
            baseXp = SkillConfig.getFarmerXpBeetroot();
        else if (block == Blocks.NETHER_WART)
            baseXp = SkillConfig.getFarmerXpNetherWart();

        if (baseXp > 0) {
            // +25% of base (minimum 1 XP)
            double multiplier = com.murilloskills.data.XpDataManager.getValues().farmer.plantingMultiplier;
            int plantXp = Math.max(1, (int) (baseXp * multiplier));
            return new SkillReceptorResult(true, plantXp);
        }

        return new SkillReceptorResult(false, 0);
    }

    /**
     * Gets XP for composting items (15% of base XP, fixed at 2 XP).
     * 
     * @return SkillReceptorResult with XP info
     */
    public static SkillReceptorResult getCompostingXp() {
        int xp = com.murilloskills.data.XpDataManager.getValues().farmer.compostingXp;
        return new SkillReceptorResult(true, xp);
    }

    /**
     * Gets XP for creating Bone Meal from composter (10% of base XP, fixed at 1
     * XP).
     * 
     * @return SkillReceptorResult with XP info
     */
    public static SkillReceptorResult getBoneMealCreationXp() {
        int xp = com.murilloskills.data.XpDataManager.getValues().farmer.boneMealXp;
        return new SkillReceptorResult(true, xp);
    }

    /**
     * Checks if a block is a crop block (any growth stage).
     * 
     * @param block The block to check
     * @return true if the block is a crop
     */
    public static boolean isCropBlock(Block block) {
        return block == Blocks.WHEAT ||
                block == Blocks.CARROTS ||
                block == Blocks.POTATOES ||
                block == Blocks.BEETROOTS ||
                block == Blocks.MELON ||
                block == Blocks.PUMPKIN ||
                block == Blocks.NETHER_WART ||
                block == Blocks.SWEET_BERRY_BUSH ||
                block == Blocks.COCOA;
    }

    /**
     * Checks if a crop block is mature (ready to harvest).
     * 
     * @param block The crop block
     * @param age   The current age/growth stage of the crop
     * @return true if the crop is fully grown
     */
    public static boolean isCropMature(Block block, int age) {
        // Wheat, Carrots, Potatoes - age 7 is mature
        if (block == Blocks.WHEAT || block == Blocks.CARROTS || block == Blocks.POTATOES) {
            return age >= 7;
        }
        // Beetroots - age 3 is mature
        else if (block == Blocks.BEETROOTS) {
            return age >= 3;
        }
        // Nether Wart - age 3 is mature
        else if (block == Blocks.NETHER_WART) {
            return age >= 3;
        }
        // Sweet Berries - age 3 is mature (harvestable at 2+)
        else if (block == Blocks.SWEET_BERRY_BUSH) {
            return age >= 2;
        }
        // Cocoa - age 2 is mature
        else if (block == Blocks.COCOA) {
            return age >= 2;
        }
        // Melon and Pumpkin are always "mature" when harvested as blocks
        else if (block == Blocks.MELON || block == Blocks.PUMPKIN) {
            return true;
        }

        return false;
    }
}
