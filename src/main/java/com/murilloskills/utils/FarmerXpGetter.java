package com.murilloskills.utils;

import com.murilloskills.data.XpDataManager;
import com.murilloskills.models.SkillReceptorResult;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

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
    private static final TagKey<Block> TAG_CROPS = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "crops"));
    private static final TagKey<Block> TAG_WHEAT = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "crops/wheat"));
    private static final TagKey<Block> TAG_CARROT = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "crops/carrot"));
    private static final TagKey<Block> TAG_POTATO = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "crops/potato"));
    private static final TagKey<Block> TAG_BEETROOT = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c",
            "crops/beetroot"));
    private static final TagKey<Block> TAG_MELON = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "crops/melon"));
    private static final TagKey<Block> TAG_PUMPKIN = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "crops/pumpkin"));
    private static final TagKey<Block> TAG_NETHER_WART = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c",
            "crops/nether_wart"));
    private static final TagKey<Block> TAG_SWEET_BERRY = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c",
            "crops/sweet_berry"));
    private static final TagKey<Block> TAG_COCOA = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "crops/cocoa"));

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

        int xp = 0;
        if (block.getRegistryEntry().isIn(TAG_WHEAT)) {
            xp = SkillConfig.getFarmerXpWheat();
        } else if (block.getRegistryEntry().isIn(TAG_CARROT)) {
            xp = SkillConfig.getFarmerXpCarrot();
        } else if (block.getRegistryEntry().isIn(TAG_POTATO)) {
            xp = SkillConfig.getFarmerXpPotato();
        } else if (block.getRegistryEntry().isIn(TAG_BEETROOT)) {
            xp = SkillConfig.getFarmerXpBeetroot();
        } else if (block.getRegistryEntry().isIn(TAG_MELON)) {
            xp = SkillConfig.getFarmerXpMelon();
        } else if (block.getRegistryEntry().isIn(TAG_PUMPKIN)) {
            xp = SkillConfig.getFarmerXpPumpkin();
        } else if (block.getRegistryEntry().isIn(TAG_NETHER_WART)) {
            xp = SkillConfig.getFarmerXpNetherWart();
        } else if (block.getRegistryEntry().isIn(TAG_SWEET_BERRY)) {
            xp = SkillConfig.getFarmerXpSweetBerry();
        } else if (block.getRegistryEntry().isIn(TAG_COCOA)) {
            xp = SkillConfig.getFarmerXpCocoa();
        }

        int overrideXp = XpDataManager.getBlockXp("farmer", block);
        if (overrideXp > 0) {
            xp = overrideXp;
        }

        if (xp > 0) {
            return new SkillReceptorResult(true, xp);
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

        if (block.getRegistryEntry().isIn(TAG_WHEAT))
            baseXp = SkillConfig.getFarmerXpWheat();
        else if (block.getRegistryEntry().isIn(TAG_CARROT))
            baseXp = SkillConfig.getFarmerXpCarrot();
        else if (block.getRegistryEntry().isIn(TAG_POTATO))
            baseXp = SkillConfig.getFarmerXpPotato();
        else if (block.getRegistryEntry().isIn(TAG_BEETROOT))
            baseXp = SkillConfig.getFarmerXpBeetroot();
        else if (block.getRegistryEntry().isIn(TAG_NETHER_WART))
            baseXp = SkillConfig.getFarmerXpNetherWart();

        int overrideXp = XpDataManager.getBlockXp("farmer", block);
        if (overrideXp > 0) {
            baseXp = overrideXp;
        }

        if (baseXp > 0) {
            // +25% of base (minimum 1 XP)
            int plantXp = Math.max(1, (int) (baseXp * 0.25));
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
        return new SkillReceptorResult(true, 2);
    }

    /**
     * Gets XP for creating Bone Meal from composter (10% of base XP, fixed at 1
     * XP).
     * 
     * @return SkillReceptorResult with XP info
     */
    public static SkillReceptorResult getBoneMealCreationXp() {
        return new SkillReceptorResult(true, 1);
    }

    /**
     * Checks if a block is a crop block (any growth stage).
     * 
     * @param block The block to check
     * @return true if the block is a crop
     */
    public static boolean isCropBlock(Block block) {
        return block.getRegistryEntry().isIn(TAG_CROPS);
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
