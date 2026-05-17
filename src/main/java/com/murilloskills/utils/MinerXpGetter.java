package com.murilloskills.utils;

import com.murilloskills.models.SkillReceptorResult;
import com.murilloskills.core.compat.CrossModCompatRules;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;

public class MinerXpGetter {

    // Base XP values for different ores
    // Note: Constants replaced by SkillConfig getters

    public static SkillReceptorResult isMinerXpBlock(Block block, boolean hasSilkTouch,
            boolean excludeStoneAndDeepSlate) {

        // Generic blocks (stone, deepslate) always grant XP, regardless of Silk Touch
        // Silk Touch only blocks XP from ores (since it prevents the ore drop XP)
        if (!excludeStoneAndDeepSlate && isBasicMiningBlock(block)) {
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
        } else if (CrossModCompatRules.isOreResourceId(Registries.BLOCK.getId(block).toString())) {
            return new SkillReceptorResult(true, SkillConfig.getMinerXpIron());
        } else {
            // NULL OR NOT ORE BLOCK
            return new SkillReceptorResult(false, 0);
        }
    }

    public static boolean isDetectableOreBlock(Block block) {
        if (isMinerXpBlock(block, false, true).didGainXp()) {
            return true;
        }
        if (isRawResourceBlock(block)) {
            return true;
        }
        return isLikelyOreId(Registries.BLOCK.getId(block).toString());
    }

    public static boolean isOreResourceId(String blockId) {
        return isLikelyOreId(blockId) || isRawResourceBlockId(blockId);
    }

    public static boolean isLikelyOreId(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return false;
        }

        String path = blockId;
        int namespaceSeparator = path.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator < path.length() - 1) {
            path = path.substring(namespaceSeparator + 1);
        }

        return path.endsWith("_ore")
                || path.contains("_ore_")
                || path.equals("ancient_debris")
                || path.endsWith("_crystal_ore")
                || path.endsWith("_gem_ore")
                || path.endsWith("_cluster")
                || path.endsWith("_deposit");
    }

    public static boolean isRawResourceBlock(Block block) {
        return block == Blocks.RAW_IRON_BLOCK
                || block == Blocks.RAW_COPPER_BLOCK
                || block == Blocks.RAW_GOLD_BLOCK
                || isRawResourceBlockId(Registries.BLOCK.getId(block).toString());
    }

    public static boolean isRawResourceBlockId(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return false;
        }

        String path = blockId;
        int namespaceSeparator = path.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator < path.length() - 1) {
            path = path.substring(namespaceSeparator + 1);
        }

        return CrossModCompatRules.isRawResourceBlockId(blockId);
    }

    public static String humanizeModdedOreName(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return "Unknown Ore";
        }

        String namespace = "minecraft";
        String path = blockId;
        int namespaceSeparator = blockId.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator < blockId.length() - 1) {
            namespace = blockId.substring(0, namespaceSeparator);
            path = blockId.substring(namespaceSeparator + 1);
        }

        String cleanedPath = path
                .replaceFirst("^deepslate_", "")
                .replaceFirst("_ore$", "")
                .replace('_', ' ');
        String oreName = capitalizeWords(cleanedPath);
        String modName = capitalizeWords(namespace.replace('_', ' '));
        return oreName + " (" + modName + ")";
    }

    private static String capitalizeWords(String value) {
        String[] words = value.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1).toLowerCase(java.util.Locale.ROOT));
            }
        }
        return result.length() == 0 ? value : result.toString();
    }

    private static boolean isBasicMiningBlock(Block block) {
        return block == Blocks.STONE
                || block == Blocks.DEEPSLATE
                || block == Blocks.COBBLESTONE
                || block == Blocks.COBBLED_DEEPSLATE;
    }

}
