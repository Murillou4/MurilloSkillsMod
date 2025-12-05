package com.murilloskills.utils;

import com.murilloskills.models.SkillReceptorResult;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

public class MinerXpGetter {

    // Base XP values for different ores
    private static final int XP_STONE = 1;
    private static final int XP_COAL = 5;
    private static final int XP_COPPER = 5;
    private static final int XP_IRON = 10;
    private static final int XP_GOLD = 15;
    private static final int XP_LAPIS = 10;
    private static final int XP_REDSTONE = 10;
    private static final int XP_DIAMOND = 40;
    private static final int XP_EMERALD = 60;
    private static final int XP_ANCIENT_DEBRIS = 150;
    private static final int XP_NETHER_QUARTZ = 5;
    private static final int XP_NETHER_GOLD = 5;

    public static SkillReceptorResult isMinerXpBlock(Block block, boolean hasSilkTouch,
            boolean excludeStoneAndDeepSlate) {

        if (hasSilkTouch) {
            return new SkillReceptorResult(false, 0);
        } else if ((block == Blocks.STONE || block == Blocks.DEEPSLATE) && !excludeStoneAndDeepSlate) {
            return new SkillReceptorResult(true, XP_STONE);
        } else if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) {
            return new SkillReceptorResult(true, XP_COAL);
        } else if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) {
            return new SkillReceptorResult(true, XP_COPPER);
        } else if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
            return new SkillReceptorResult(true, XP_IRON);
        } else if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) {
            return new SkillReceptorResult(true, XP_GOLD);
        } else if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) {
            return new SkillReceptorResult(true, XP_LAPIS);
        } else if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
            return new SkillReceptorResult(true, XP_REDSTONE);
        } else if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            return new SkillReceptorResult(true, XP_DIAMOND);
        } else if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) {
            return new SkillReceptorResult(true, XP_EMERALD);
        } else if (block == Blocks.ANCIENT_DEBRIS) {
            return new SkillReceptorResult(true, XP_ANCIENT_DEBRIS);
        }
        // NETHER ORES
        else if (block == Blocks.NETHER_QUARTZ_ORE) {
            return new SkillReceptorResult(true, XP_NETHER_QUARTZ);
        } else if (block == Blocks.NETHER_GOLD_ORE) {
            return new SkillReceptorResult(true, XP_NETHER_GOLD);
        } else {
            // NULL OR NOT ORE BLOCK
            return new SkillReceptorResult(false, 0);
        }
    }

}
