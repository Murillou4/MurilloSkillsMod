package com.murilloskills.utils;

import com.murilloskills.models.SkillReceptorResult;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

public class MinerXpGetter {

    public static SkillReceptorResult isMinerXpBlock(Block block, boolean hasSilkTouch, boolean excludeStoneAndDeepSlate) {


        if (hasSilkTouch) {
            return new SkillReceptorResult(false, 0);
        } else if ((block == Blocks.STONE || block == Blocks.DEEPSLATE) && !excludeStoneAndDeepSlate) {
            return new SkillReceptorResult(true, 1);
        } else if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) {
            return new SkillReceptorResult(true, 5);
        } else if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) {
            return new SkillReceptorResult(true, 5);
        } else if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
            return new SkillReceptorResult(true, 10);
        } else if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) {
            return new SkillReceptorResult(true, 15);
        } else if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) {
            return new SkillReceptorResult(true, 10);
        } else if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
            return new SkillReceptorResult(true, 10);
        } else if (block == Blocks.DIAMOND_BLOCK || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            return new SkillReceptorResult(true, 40);
        } else if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) {
            return new SkillReceptorResult(true, 60);
        } else if (block == Blocks.ANCIENT_DEBRIS) {
            return new SkillReceptorResult(true, 1500000);
        }
        //NETHER ORES
        else if (block == Blocks.NETHER_QUARTZ_ORE || block == Blocks.NETHER_GOLD_ORE) {
            return new SkillReceptorResult(true, 5);
        } else {
            //NULL OR NOT ORE BLOCK
            return new SkillReceptorResult(false, 0);
        }
    }

}
