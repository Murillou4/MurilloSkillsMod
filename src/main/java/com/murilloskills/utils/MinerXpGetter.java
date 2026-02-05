package com.murilloskills.utils;

import com.murilloskills.models.SkillReceptorResult;
import com.murilloskills.data.XpDataManager;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class MinerXpGetter {

    // Base XP values for different ores
    // Note: Constants replaced by SkillConfig getters
    private static final TagKey<Block> TAG_ORES = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores"));
    private static final TagKey<Block> TAG_COAL = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/coal"));
    private static final TagKey<Block> TAG_COPPER = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/copper"));
    private static final TagKey<Block> TAG_IRON = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/iron"));
    private static final TagKey<Block> TAG_GOLD = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/gold"));
    private static final TagKey<Block> TAG_LAPIS = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/lapis"));
    private static final TagKey<Block> TAG_REDSTONE = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/redstone"));
    private static final TagKey<Block> TAG_DIAMOND = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/diamond"));
    private static final TagKey<Block> TAG_EMERALD = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/emerald"));
    private static final TagKey<Block> TAG_QUARTZ = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/quartz"));
    private static final TagKey<Block> TAG_NETHER_GOLD = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/nether_gold"));
    private static final TagKey<Block> TAG_ANCIENT_DEBRIS = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c",
            "ores/ancient_debris"));

    public static SkillReceptorResult isMinerXpBlock(Block block, boolean hasSilkTouch,
            boolean excludeStoneAndDeepSlate) {

        // Generic blocks (stone, deepslate) always grant XP, regardless of Silk Touch
        // Silk Touch only blocks XP from ores (since it prevents the ore drop XP)
        if ((block == Blocks.STONE || block == Blocks.DEEPSLATE) && !excludeStoneAndDeepSlate) {
            int stoneXp = XpDataManager.getBlockXp("miner", block);
            if (stoneXp <= 0) {
                stoneXp = SkillConfig.getMinerXpStone();
            }
            return new SkillReceptorResult(true, stoneXp);
        }

        // For ore blocks, Silk Touch blocks XP gain (since you're not actually mining
        // the ore)
        if (hasSilkTouch && block.getRegistryEntry().isIn(TAG_ORES)) {
            return new SkillReceptorResult(false, 0);
        }

        int xp = 0;
        if (block.getRegistryEntry().isIn(TAG_COAL)) {
            xp = SkillConfig.getMinerXpCoal();
        } else if (block.getRegistryEntry().isIn(TAG_COPPER)) {
            xp = SkillConfig.getMinerXpCopper();
        } else if (block.getRegistryEntry().isIn(TAG_IRON)) {
            xp = SkillConfig.getMinerXpIron();
        } else if (block.getRegistryEntry().isIn(TAG_GOLD)) {
            xp = SkillConfig.getMinerXpGold();
        } else if (block.getRegistryEntry().isIn(TAG_LAPIS)) {
            xp = SkillConfig.getMinerXpLapis();
        } else if (block.getRegistryEntry().isIn(TAG_REDSTONE)) {
            xp = SkillConfig.getMinerXpRedstone();
        } else if (block.getRegistryEntry().isIn(TAG_DIAMOND)) {
            xp = SkillConfig.getMinerXpDiamond();
        } else if (block.getRegistryEntry().isIn(TAG_EMERALD)) {
            xp = SkillConfig.getMinerXpEmerald();
        } else if (block.getRegistryEntry().isIn(TAG_ANCIENT_DEBRIS)) {
            xp = SkillConfig.getMinerXpAncientDebris();
        } else if (block.getRegistryEntry().isIn(TAG_QUARTZ)) {
            xp = SkillConfig.getMinerXpNetherQuartz();
        } else if (block.getRegistryEntry().isIn(TAG_NETHER_GOLD)) {
            xp = SkillConfig.getMinerXpNetherGold();
        }

        int overrideXp = XpDataManager.getBlockXp("miner", block);
        if (overrideXp > 0) {
            xp = overrideXp;
        }

        if (xp > 0) {
            return new SkillReceptorResult(true, xp);
        }

        return new SkillReceptorResult(false, 0);
    }

}
