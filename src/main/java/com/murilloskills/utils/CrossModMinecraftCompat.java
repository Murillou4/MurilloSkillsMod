package com.murilloskills.utils;

import com.murilloskills.core.compat.CrossModCompatRules;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;

public final class CrossModMinecraftCompat {
    private CrossModMinecraftCompat() {
    }

    public static String blockId(Block block) {
        return Registries.BLOCK.getId(block).toString();
    }

    public static String itemId(net.minecraft.item.Item item) {
        return Registries.ITEM.getId(item).toString();
    }

    public static boolean isPlantLike(BlockState state) {
        Block block = state.getBlock();
        String id = blockId(block);
        if (CrossModCompatRules.isPlantLikeId(id)) {
            return true;
        }
        try {
            if (state.isIn(BlockTags.SAPLINGS) || state.isIn(BlockTags.LEAVES) || state.isIn(BlockTags.FLOWERS)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        String cls = block.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        return cls.contains("crop")
                || cls.contains("plant")
                || cls.contains("sapling")
                || cls.contains("stem")
                || cls.contains("bush")
                || cls.contains("cactus")
                || cls.contains("kelp")
                || cls.contains("vine")
                || cls.contains("fungus")
                || cls.contains("mushroom");
    }

    public static boolean isHarvestablePlant(BlockState state) {
        return isPlantLike(state) || CrossModCompatRules.isHarvestablePlantId(blockId(state.getBlock()));
    }

    public static boolean isAreaPlantableBlock(Block block) {
        BlockState state = block.getDefaultState();
        return isPlantLike(state) || CrossModCompatRules.isSaplingLikeId(blockId(block));
    }

    public static boolean isMaturePlant(BlockState state) {
        IntProperty age = findAgeProperty(state);
        if (age == null) {
            return isHarvestablePlant(state) && CrossModCompatRules.isHarvestablePlantId(blockId(state.getBlock()));
        }
        Integer current = state.get(age);
        return current != null && current.intValue() >= maxValue(age);
    }

    public static BlockState growBy(BlockState state, int amount) {
        if (amount <= 0 || !isPlantLike(state)) {
            return state;
        }
        IntProperty age = findAgeProperty(state);
        if (age == null) {
            return state;
        }
        int current = state.get(age);
        int next = Math.min(maxValue(age), current + amount);
        return next > current ? state.with(age, next) : state;
    }

    public static int genericFarmerHarvestXp(Block block) {
        String id = blockId(block);
        String path = CrossModCompatRules.path(id);
        if (path.contains("wart") || path.contains("cocoa") || path.contains("berry")) {
            return Math.max(1, SkillConfig.getFarmerXpNetherWart());
        }
        if (path.contains("melon") || path.contains("pumpkin") || path.contains("gourd")) {
            return Math.max(1, SkillConfig.getFarmerXpMelon());
        }
        if (path.contains("cane") || path.contains("cactus") || path.contains("bamboo") || path.contains("kelp")) {
            return Math.max(1, SkillConfig.getFarmerXpSugarCane());
        }
        if (CrossModCompatRules.isHarvestablePlantId(id)) {
            return Math.max(1, SkillConfig.getFarmerXpWheat());
        }
        return 0;
    }

    public static int genericFarmerPlantXp(Block block) {
        int harvestXp = genericFarmerHarvestXp(block);
        return harvestXp <= 0 ? 0 : Math.max(1, Math.round(harvestXp * 0.25f));
    }

    public static CrossModCompatRules.BuilderCategory builderCategory(Block block) {
        return CrossModCompatRules.builderCategory(blockId(block));
    }

    public static boolean isLootContainer(Block block) {
        return CrossModCompatRules.isLootContainerId(blockId(block));
    }

    private static IntProperty findAgeProperty(BlockState state) {
        IntProperty fallback = null;
        for (Property<?> property : state.getProperties()) {
            if (!(property instanceof IntProperty intProperty)) {
                continue;
            }
            String name = intProperty.getName().toLowerCase(java.util.Locale.ROOT);
            if (name.equals("age")) {
                return intProperty;
            }
            if (name.contains("age") || name.contains("growth") || name.contains("stage")
                    || name.contains("maturity")) {
                fallback = intProperty;
            }
        }
        return fallback;
    }

    private static int maxValue(IntProperty property) {
        int max = Integer.MIN_VALUE;
        for (Integer value : property.getValues()) {
            max = Math.max(max, value.intValue());
        }
        return max == Integer.MIN_VALUE ? 0 : max;
    }
}
