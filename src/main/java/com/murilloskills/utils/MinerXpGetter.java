package com.murilloskills.utils;

import com.murilloskills.models.SkillReceptorResult;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

public class MinerXpGetter {

    // Base XP values for different ores
    // Note: Constants replaced by SkillConfig getters

    public static SkillReceptorResult isMinerXpBlock(Block block, boolean hasSilkTouch,
            boolean excludeStoneAndDeepSlate) {

        // Generic blocks (stone, deepslate) always grant XP, regardless of Silk Touch
        // Silk Touch only blocks XP from ores (since it prevents the ore drop XP)
        if ((block == Blocks.STONE || block == Blocks.DEEPSLATE) && !excludeStoneAndDeepSlate) {
            return new SkillReceptorResult(true, SkillConfig.getMinerXpStone());
        }

        // For ore blocks, Silk Touch blocks XP gain (since you're not actually mining
        // the ore)
        if (hasSilkTouch) {
            return new SkillReceptorResult(false, 0);
        }

        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) {
            return new SkillReceptorResult(true, SkillConfig.getMinerXpCoal());
        } else if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) {
            return new SkillReceptorResult(true, SkillConfig.getMinerXpCopper());
        } else if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
            return new SkillReceptorResult(true, SkillConfig.getMinerXpIron());
        } else if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) {
            return new SkillReceptorResult(true, SkillConfig.getMinerXpGold());
        } else if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) {
            return new SkillReceptorResult(true, SkillConfig.getMinerXpLapis());
        } else if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
            return new SkillReceptorResult(true, SkillConfig.getMinerXpRedstone());
        } else if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            return new SkillReceptorResult(true, SkillConfig.getMinerXpDiamond());
        } else if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) {
            return new SkillReceptorResult(true, SkillConfig.getMinerXpEmerald());
        } else if (block == Blocks.ANCIENT_DEBRIS) {
            return new SkillReceptorResult(true, SkillConfig.getMinerXpAncientDebris());
        }
        // NETHER ORES
        else if (block == Blocks.NETHER_QUARTZ_ORE) {
            return new SkillReceptorResult(true, SkillConfig.getMinerXpNetherQuartz());
        } else if (block == Blocks.NETHER_GOLD_ORE) {
            return new SkillReceptorResult(true, SkillConfig.getMinerXpNetherGold());
        } else {
            // NULL OR NOT ORE BLOCK
            return new SkillReceptorResult(false, 0);
        }
    }

}
