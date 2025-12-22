package com.murilloskills.utils;

import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks progress for advancement achievements that require counting.
 * Uses player NBT data to persist counts across sessions.
 */
public class AchievementTracker {

    // Achievement thresholds
    public static final int EMERALD_HUNTER_COUNT = 64;
    public static final int MEGA_FARMER_COUNT = 1000;
    public static final int SEA_KING_COUNT = 500;
    public static final int FIRST_BUILD_COUNT = 100;
    public static final int DIVINE_ARCHITECT_COUNT = 10000;
    public static final int TRAVELER_DISTANCE = 10000;
    public static final int BIOME_DISCOVERER_COUNT = 10;
    public static final int SNIPER_DISTANCE = 50;

    // New achievement thresholds
    public static final int MASTER_ENCHANTER_COUNT = 50;
    public static final int ELITE_HUNTER_COUNT = 100;
    public static final int ANIMAL_BREEDER_COUNT = 50;
    public static final int PRECISION_MASTER_COUNT = 10;
    public static final int LUCKY_FISHER_COUNT = 10;
    public static final int SPEED_LEVELER_COUNT = 5;
    public static final int XP_STREAK_COUNT = 10;

    // Achievement keys
    public static final String KEY_EMERALDS_MINED = "emeralds_mined";
    public static final String KEY_CROPS_HARVESTED = "crops_harvested";
    public static final String KEY_FISH_CAUGHT = "fish_caught";
    public static final String KEY_BLOCKS_PLACED = "blocks_placed";
    public static final String KEY_DISTANCE_TRAVELED = "distance_traveled";
    public static final String KEY_BIOMES_DISCOVERED = "biomes_discovered";
    public static final String KEY_HEADSHOTS = "headshots";
    public static final String KEY_LONG_SHOTS = "long_shots";
    public static final String KEY_TREASURES_FISHED = "treasures_fished";

    // New achievement keys
    public static final String KEY_ITEMS_ENCHANTED = "items_enchanted";
    public static final String KEY_MOBS_KILLED = "mobs_killed";
    public static final String KEY_ANIMALS_BRED = "animals_bred";
    public static final String KEY_LEVELS_TODAY = "levels_today";
    public static final String KEY_MAX_STREAK = "max_streak";
    public static final String KEY_DIMENSIONS_VISITED = "dimensions_visited";

    /**
     * Increment a counter and check if advancement should be granted.
     */
    public static void incrementAndCheck(ServerPlayerEntity player, MurilloSkillsList skill, String key, int amount) {
        MinecraftServer server = (MinecraftServer) player.getEntityWorld().getServer();
        if (server == null)
            return;

        PlayerSkillData data = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
        if (data == null)
            return;

        // Get or create stats map
        Map<String, Integer> stats = data.achievementStats;
        if (stats == null) {
            stats = new HashMap<>();
            data.achievementStats = stats;
        }

        // Increment counter
        int currentCount = stats.getOrDefault(key, 0);
        int newCount = currentCount + amount;
        stats.put(key, newCount);
        stats.put(key, newCount);

        // Check and grant achievements based on key and skill
        checkAndGrantAchievement(player, skill, key, newCount);
    }

    /**
     * Check if progress triggers an advancement.
     */
    private static void checkAndGrantAchievement(ServerPlayerEntity player, MurilloSkillsList skill, String key,
            int count) {
        switch (key) {
            case KEY_EMERALDS_MINED:
                if (count >= EMERALD_HUNTER_COUNT) {
                    AdvancementGranter.grantEmeraldHunter(player);
                }
                break;

            case KEY_CROPS_HARVESTED:
                if (count >= MEGA_FARMER_COUNT) {
                    AdvancementGranter.grantMegaFarmer(player);
                }
                break;

            case KEY_FISH_CAUGHT:
                if (count >= SEA_KING_COUNT) {
                    AdvancementGranter.grantSeaKing(player);
                }
                break;

            case KEY_BLOCKS_PLACED:
                if (count >= FIRST_BUILD_COUNT) {
                    AdvancementGranter.grantFirstBuild(player);
                }
                if (count >= DIVINE_ARCHITECT_COUNT) {
                    AdvancementGranter.grantDivineArchitect(player);
                }
                break;

            case KEY_DISTANCE_TRAVELED:
                if (count >= TRAVELER_DISTANCE) {
                    AdvancementGranter.grantTraveler(player);
                }
                break;

            case KEY_BIOMES_DISCOVERED:
                if (count >= BIOME_DISCOVERER_COUNT) {
                    AdvancementGranter.grantBiomeDiscoverer(player);
                }
                break;

            case KEY_TREASURES_FISHED:
                // First treasure is enough for the base achievement
                if (count >= 1) {
                    AdvancementGranter.grantTreasureCatch(player);
                }
                if (count >= LUCKY_FISHER_COUNT) {
                    AdvancementGranter.grantLuckyFisher(player);
                }
                break;

            case KEY_HEADSHOTS:
                if (count >= 1) {
                    AdvancementGranter.grantSharpshooter(player);
                }
                if (count >= PRECISION_MASTER_COUNT) {
                    AdvancementGranter.grantPrecisionMaster(player);
                }
                break;

            case KEY_LONG_SHOTS:
                if (count >= 1) {
                    AdvancementGranter.grantSniper(player);
                }
                break;

            // New achievements
            case KEY_ITEMS_ENCHANTED:
                if (count >= MASTER_ENCHANTER_COUNT) {
                    AdvancementGranter.grantMasterEnchanter(player);
                }
                break;

            case KEY_MOBS_KILLED:
                if (count >= ELITE_HUNTER_COUNT) {
                    AdvancementGranter.grantEliteHunter(player);
                }
                break;

            case KEY_ANIMALS_BRED:
                if (count >= ANIMAL_BREEDER_COUNT) {
                    AdvancementGranter.grantAnimalBreeder(player);
                }
                break;

            case KEY_MAX_STREAK:
                if (count >= XP_STREAK_COUNT) {
                    AdvancementGranter.grantXpStreak(player);
                }
                break;

            case KEY_LEVELS_TODAY:
                if (count >= SPEED_LEVELER_COUNT) {
                    AdvancementGranter.grantSpeedLeveler(player);
                }
                break;
        }
    }

    /**
     * Get current count for a key.
     */
    public static int getCount(ServerPlayerEntity player, String key) {
        MinecraftServer server = (MinecraftServer) player.getEntityWorld().getServer();
        if (server == null)
            return 0;

        var data = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
        if (data == null || data.achievementStats == null)
            return 0;

        return data.achievementStats.getOrDefault(key, 0);
    }
}
