package com.murilloskills.utils;

/**
 * Utility class to get XP values for Explorer skill actions.
 */
public class ExplorerXpGetter {

    // XP values from SkillConfig
    private static final int XP_BIOME = SkillConfig.EXPLORER_XP_BIOME;
    private static final int XP_STRUCTURE = SkillConfig.EXPLORER_XP_STRUCTURE;
    private static final int XP_LOOT_CHEST = SkillConfig.EXPLORER_XP_LOOT_CHEST;
    private static final int XP_MAP_COMPLETE = SkillConfig.EXPLORER_XP_MAP_COMPLETE;
    private static final int XP_WANDERING_TRADE = SkillConfig.EXPLORER_XP_WANDERING_TRADE;

    /**
     * Gets XP for discovering a new biome.
     */
    public static int getBiomeDiscoveryXp() {
        return XP_BIOME;
    }

    /**
     * Gets XP for discovering a new structure.
     */
    public static int getStructureDiscoveryXp() {
        return XP_STRUCTURE;
    }

    /**
     * Gets XP for opening a loot chest for the first time.
     */
    public static int getLootChestXp() {
        return XP_LOOT_CHEST;
    }

    /**
     * Gets XP for completing a map.
     */
    public static int getMapCompleteXp() {
        return XP_MAP_COMPLETE;
    }

    /**
     * Gets XP for trading with a Wandering Trader.
     */
    public static int getWanderingTradeXp() {
        return XP_WANDERING_TRADE;
    }

    /**
     * Gets XP for traveling a certain distance.
     */
    public static int getDistanceXp() {
        return SkillConfig.EXPLORER_XP_PER_DISTANCE;
    }
}
