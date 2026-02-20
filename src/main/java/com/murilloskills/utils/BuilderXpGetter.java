package com.murilloskills.utils;

import com.murilloskills.models.SkillReceptorResult;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;

import java.util.Set;

/**
 * Utility class to determine XP rewards for Builder skill actions.
 * 
 * XP Sources:
 * - Placing construction blocks: Base XP by category
 * - Crafting structural/decorative blocks: Base XP
 * 
 * Block Categories:
 * - Structural: Stone, Bricks, Concrete, Deepslate, Wood → Higher XP
 * - Decorative: Stairs, Slabs, Fences, Walls, Glass → Medium XP
 * - Basic: Dirt, Cobblestone, etc. → Lower XP
 */
public class BuilderXpGetter {

    // XP values by block category
    // Note: Constants replaced by SkillConfig getters

    // Set of structural blocks that give higher XP
    private static final Set<Block> STRUCTURAL_BLOCKS = Set.of(
            Blocks.STONE, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS,
            Blocks.CRACKED_STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS,
            Blocks.DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.POLISHED_DEEPSLATE,
            Blocks.DEEPSLATE_TILES, Blocks.COBBLED_DEEPSLATE,
            Blocks.BRICKS, Blocks.MUD_BRICKS,
            Blocks.SANDSTONE, Blocks.RED_SANDSTONE, Blocks.SMOOTH_SANDSTONE,
            Blocks.WHITE_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.MAGENTA_CONCRETE,
            Blocks.LIGHT_BLUE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE,
            Blocks.PINK_CONCRETE, Blocks.GRAY_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE,
            Blocks.CYAN_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.BLUE_CONCRETE,
            Blocks.BROWN_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.RED_CONCRETE,
            Blocks.BLACK_CONCRETE,
            Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS,
            Blocks.JUNGLE_PLANKS, Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS,
            Blocks.MANGROVE_PLANKS, Blocks.CHERRY_PLANKS, Blocks.BAMBOO_PLANKS,
            Blocks.CRIMSON_PLANKS, Blocks.WARPED_PLANKS,
            Blocks.POLISHED_ANDESITE, Blocks.POLISHED_GRANITE, Blocks.POLISHED_DIORITE);

    // Set of premium blocks that give highest XP
    private static final Set<Block> PREMIUM_BLOCKS = Set.of(
            Blocks.QUARTZ_BLOCK, Blocks.SMOOTH_QUARTZ, Blocks.QUARTZ_BRICKS,
            Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_PILLAR,
            Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.DARK_PRISMARINE,
            Blocks.SEA_LANTERN, Blocks.END_STONE_BRICKS,
            Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR,
            Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA,
            Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA,
            Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA,
            Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA,
            Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA,
            Blocks.BLACK_TERRACOTTA, Blocks.TERRACOTTA,
            Blocks.WHITE_GLAZED_TERRACOTTA, Blocks.ORANGE_GLAZED_TERRACOTTA,
            Blocks.MAGENTA_GLAZED_TERRACOTTA, Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA,
            Blocks.YELLOW_GLAZED_TERRACOTTA, Blocks.LIME_GLAZED_TERRACOTTA,
            Blocks.PINK_GLAZED_TERRACOTTA, Blocks.GRAY_GLAZED_TERRACOTTA,
            Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA, Blocks.CYAN_GLAZED_TERRACOTTA,
            Blocks.PURPLE_GLAZED_TERRACOTTA, Blocks.BLUE_GLAZED_TERRACOTTA,
            Blocks.BROWN_GLAZED_TERRACOTTA, Blocks.GREEN_GLAZED_TERRACOTTA,
            Blocks.RED_GLAZED_TERRACOTTA, Blocks.BLACK_GLAZED_TERRACOTTA,
            Blocks.COPPER_BLOCK, Blocks.CUT_COPPER, Blocks.EXPOSED_COPPER,
            Blocks.WEATHERED_COPPER, Blocks.OXIDIZED_COPPER);

    // Set of decorative blocks
    private static final Set<Block> DECORATIVE_BLOCKS = Set.of(
            Blocks.GLASS, Blocks.WHITE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS,
            Blocks.MAGENTA_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS,
            Blocks.YELLOW_STAINED_GLASS, Blocks.LIME_STAINED_GLASS,
            Blocks.PINK_STAINED_GLASS, Blocks.GRAY_STAINED_GLASS,
            Blocks.LIGHT_GRAY_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS,
            Blocks.PURPLE_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS,
            Blocks.BROWN_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS,
            Blocks.RED_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS,
            Blocks.GLOWSTONE, Blocks.SHROOMLIGHT, Blocks.OCHRE_FROGLIGHT,
            Blocks.VERDANT_FROGLIGHT, Blocks.PEARLESCENT_FROGLIGHT,
            Blocks.LANTERN, Blocks.SOUL_LANTERN);

    // Set of basic blocks that give minimal XP
    private static final Set<Block> BASIC_BLOCKS = Set.of(
            Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COBBLESTONE,
            Blocks.SAND, Blocks.GRAVEL, Blocks.NETHERRACK,
            Blocks.MOSSY_COBBLESTONE);

    /**
     * Gets XP for placing a block.
     * 
     * @param block The block being placed
     * @return SkillReceptorResult with XP info
     */
    public static SkillReceptorResult getBlockPlacementXp(Block block) {
        // Premium blocks
        if (PREMIUM_BLOCKS.contains(block)) {
            return new SkillReceptorResult(true, SkillConfig.getBuilderXpPremium());
        }
        // Structural blocks
        if (STRUCTURAL_BLOCKS.contains(block)) {
            return new SkillReceptorResult(true, SkillConfig.getBuilderXpStructural());
        }
        // Decorative blocks or stairs/slabs/fences/walls
        if (DECORATIVE_BLOCKS.contains(block) || isDecorativeVariant(block)) {
            return new SkillReceptorResult(true, SkillConfig.getBuilderXpDecorative());
        }
        // Basic blocks
        if (BASIC_BLOCKS.contains(block)) {
            return new SkillReceptorResult(true, SkillConfig.getBuilderXpBasic());
        }
        // Default: some XP for any solid block
        try {
            if (block.getDefaultState().isSolidBlock(null, null)) {
                return new SkillReceptorResult(true, SkillConfig.getBuilderXpBasic());
            }
        } catch (Exception ignored) {
            // Some blocks (e.g., ShulkerBox) throw NPE with null world/pos
            return new SkillReceptorResult(true, SkillConfig.getBuilderXpBasic());
        }

        return new SkillReceptorResult(false, 0);
    }

    /**
     * Gets XP for crafting a block.
     * 
     * @param item The crafted item
     * @return SkillReceptorResult with XP info
     */
    public static SkillReceptorResult getCraftingXp(Item item) {
        if (isStructuralItem(item)) {
            return new SkillReceptorResult(true, SkillConfig.getBuilderXpCraftStructural());
        }
        if (isDecorativeItem(item)) {
            return new SkillReceptorResult(true, SkillConfig.getBuilderXpCraftDecorative());
        }
        return new SkillReceptorResult(false, 0);
    }

    /**
     * Checks if a block is a decorative/functional construction variant.
     * Includes: stairs, slabs, fences, walls, doors, gates, carpets, signs, etc.
     */
    public static boolean isDecorativeVariant(Block block) {
        String name = block.getTranslationKey().toLowerCase();
        return name.contains("stairs") ||
                name.contains("slab") ||
                name.contains("fence") ||
                name.contains("wall") ||
                name.contains("pane") ||
                name.contains("door") || // Includes trapdoor
                name.contains("gate") || // Fence gates
                name.contains("carpet") ||
                name.contains("button") ||
                name.contains("lever") ||
                name.contains("sign") ||
                name.contains("banner") ||
                name.contains("torch") ||
                name.contains("lantern") ||
                name.contains("chain") ||
                name.contains("ladder") ||
                name.contains("rail") ||
                name.contains("pressure_plate") ||
                name.contains("candle") ||
                name.contains("pot") || // Flower pots
                name.contains("bed") ||
                name.contains("chest") || // Chests are construction too
                name.contains("shulker"); // Shulker boxes
    }

    /**
     * Checks if an item is a structural crafting item.
     */
    public static boolean isStructuralItem(Item item) {
        String name = item.getTranslationKey().toLowerCase();
        return name.contains("brick") ||
                name.contains("concrete") ||
                name.contains("planks") ||
                name.contains("stone") ||
                name.contains("deepslate");
    }

    /**
     * Checks if an item is a decorative/functional crafting item.
     */
    public static boolean isDecorativeItem(Item item) {
        String name = item.getTranslationKey().toLowerCase();
        return name.contains("stairs") ||
                name.contains("slab") ||
                name.contains("fence") ||
                name.contains("wall") ||
                name.contains("pane") ||
                name.contains("glass") ||
                name.contains("lantern") ||
                name.contains("door") ||
                name.contains("gate") ||
                name.contains("carpet") ||
                name.contains("button") ||
                name.contains("sign") ||
                name.contains("banner") ||
                name.contains("torch") ||
                name.contains("chain") ||
                name.contains("ladder") ||
                name.contains("rail") ||
                name.contains("candle") ||
                name.contains("pot") ||
                name.contains("bed");
    }

    /**
     * Check if a block is considered a construction block (gives XP when placed).
     */
    public static boolean isConstructionBlock(Block block) {
        return PREMIUM_BLOCKS.contains(block) ||
                STRUCTURAL_BLOCKS.contains(block) ||
                DECORATIVE_BLOCKS.contains(block) ||
                isDecorativeVariant(block) ||
                BASIC_BLOCKS.contains(block);
    }
}
