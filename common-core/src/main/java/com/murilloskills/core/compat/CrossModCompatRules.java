package com.murilloskills.core.compat;

import java.util.Locale;

public final class CrossModCompatRules {
    private CrossModCompatRules() {
    }

    public enum BuilderCategory {
        NONE,
        BASIC,
        DECORATIVE,
        STRUCTURAL,
        PREMIUM
    }

    public static String path(String idOrKey) {
        if (idOrKey == null) {
            return "";
        }
        String value = idOrKey.toLowerCase(Locale.ROOT).trim();
        int colon = value.indexOf(':');
        if (colon >= 0 && colon < value.length() - 1) {
            value = value.substring(colon + 1);
        }
        int dot = value.lastIndexOf('.');
        if (dot >= 0 && dot < value.length() - 1) {
            value = value.substring(dot + 1);
        }
        return value;
    }

    public static boolean isOreResourceId(String blockId) {
        String path = path(blockId);
        return path.endsWith("_ore")
                || path.contains("_ore_")
                || path.equals("ancient_debris")
                || path.endsWith("_crystal_ore")
                || path.endsWith("_gem_ore")
                || path.endsWith("_cluster")
                || path.endsWith("_deposit")
                || isRawResourceBlockId(blockId);
    }

    public static boolean isRawResourceBlockId(String blockId) {
        String path = path(blockId);
        return path.equals("raw_iron_block")
                || path.equals("raw_copper_block")
                || path.equals("raw_gold_block")
                || (path.startsWith("raw_") && path.endsWith("_block"));
    }

    public static boolean isPlantLikeId(String blockId) {
        String path = path(blockId);
        return path.contains("crop")
                || path.contains("seed")
                || path.contains("plant")
                || path.contains("sapling")
                || path.contains("sprout")
                || path.contains("bush")
                || path.contains("berry")
                || path.contains("stem")
                || path.contains("cane")
                || path.contains("wart")
                || path.contains("cactus")
                || path.contains("kelp")
                || path.contains("bamboo")
                || path.contains("mushroom")
                || path.contains("fungus")
                || path.contains("vine")
                || path.contains("leafy")
                || path.contains("flower");
    }

    public static boolean isHarvestablePlantId(String blockId) {
        String path = path(blockId);
        return isPlantLikeId(path)
                || path.equals("melon")
                || path.equals("pumpkin")
                || path.endsWith("_melon")
                || path.endsWith("_pumpkin")
                || path.endsWith("_gourd");
    }

    public static boolean isSaplingLikeId(String blockId) {
        String path = path(blockId);
        return path.contains("sapling") || path.contains("tree_seed") || path.contains("tree_sapling");
    }

    public static boolean isLikelyMachineIdOrClass(String idOrClass) {
        String path = path(idOrClass);
        return path.contains("machine")
                || path.contains("furnace")
                || path.contains("smelter")
                || path.contains("crusher")
                || path.contains("grinder")
                || path.contains("macerator")
                || path.contains("compressor")
                || path.contains("alloy")
                || path.contains("centrifuge")
                || path.contains("pulverizer")
                || path.contains("sawmill")
                || path.contains("processor")
                || path.contains("generator")
                || path.contains("fabricator")
                || path.contains("assembler")
                || path.contains("crafter")
                || path.contains("press");
    }

    public static BuilderCategory builderCategory(String blockOrItemId) {
        String path = path(blockOrItemId);
        if (path.length() == 0) {
            return BuilderCategory.NONE;
        }
        if (containsAny(path, "quartz", "prismarine", "purpur", "terracotta", "glazed_terracotta",
                "copper_block", "cut_copper", "sea_lantern", "end_stone_brick")) {
            return BuilderCategory.PREMIUM;
        }
        if (containsAny(path, "brick", "concrete", "plank", "stone", "deepslate", "basalt", "andesite",
                "diorite", "granite", "limestone", "marble", "slate", "tile", "metal_block")) {
            return BuilderCategory.STRUCTURAL;
        }
        if (containsAny(path, "stairs", "slab", "fence", "wall", "pane", "glass", "lantern", "door",
                "trapdoor", "gate", "carpet", "button", "lever", "sign", "banner", "torch", "chain",
                "ladder", "rail", "pressure_plate", "candle", "pot", "bed", "chest", "shulker",
                "crate", "shelf", "table", "chair", "bench")) {
            return BuilderCategory.DECORATIVE;
        }
        if (containsAny(path, "dirt", "grass", "cobble", "sand", "gravel", "netherrack", "mud", "clay",
                "log", "wood", "wool", "block")) {
            return BuilderCategory.BASIC;
        }
        return BuilderCategory.NONE;
    }

    public static boolean isFishItemId(String itemId) {
        String path = path(itemId);
        return path.contains("fish")
                || path.contains("salmon")
                || path.contains("cod")
                || path.contains("tuna")
                || path.contains("trout")
                || path.contains("bass")
                || path.contains("carp")
                || path.contains("sardine")
                || path.contains("eel");
    }

    public static boolean isFishingTreasureItemId(String itemId) {
        String path = path(itemId);
        return containsAny(path, "treasure", "pearl", "shell", "nautilus", "saddle", "name_tag",
                "enchanted_book", "artifact", "relic", "gem", "crate");
    }

    public static boolean isFishingJunkItemId(String itemId) {
        String path = path(itemId);
        return containsAny(path, "junk", "stick", "string", "bone", "leather", "boot", "bowl",
                "tripwire_hook", "rotten", "trash", "scrap");
    }

    public static boolean isLootContainerId(String blockId) {
        String path = path(blockId);
        return containsAny(path, "chest", "barrel", "crate", "urn", "jar", "vase", "loot", "cache",
                "coffer", "sarcophagus", "treasure");
    }

    public static boolean isLikelyHostileEntityId(String entityIdOrClass) {
        String path = path(entityIdOrClass);
        return containsAny(path, "zombie", "skeleton", "creeper", "spider", "slime", "wraith", "ghost",
                "demon", "goblin", "raider", "bandit", "pirate", "boss", "dragon", "wither", "warden",
                "hostile", "monster");
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
