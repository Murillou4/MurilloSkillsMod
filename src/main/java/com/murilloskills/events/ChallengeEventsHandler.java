package com.murilloskills.events;

import com.murilloskills.utils.DailyChallengeManager;
import com.murilloskills.utils.DailyChallengeManager.ChallengeType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for various challenge tracking events.
 * Registers Fabric events to track player actions for daily challenges.
 */
public class ChallengeEventsHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-Challenges");

    // Track player positions for travel distance
    private static final Map<UUID, BlockPos> lastPositions = new HashMap<>();
    private static final Map<UUID, String> lastBiomes = new HashMap<>();

    /**
     * Registers all challenge tracking events.
     * Call this from the main mod initializer.
     */
    public static void register() {
        // Track travel distance and biome discovery every tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                trackTravelDistance(player);
                trackBiomeDiscovery(player);
            }
        });

        // Track chest opening
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            var blockState = world.getBlockState(pos);
            var block = blockState.getBlock();

            // Check for chests
            if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST ||
                    block == Blocks.ENDER_CHEST || block == Blocks.BARREL) {
                DailyChallengeManager.recordProgress(serverPlayer, ChallengeType.OPEN_CHESTS, 1);
                DailyChallengeManager.syncChallenges(serverPlayer);
            }

            return ActionResult.PASS;
        });

        LOGGER.info("ChallengeEventsHandler registered for daily challenges");
    }

    /**
     * Tracks travel distance for explorer challenges.
     */
    private static void trackTravelDistance(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        BlockPos currentPos = player.getBlockPos();

        if (lastPositions.containsKey(playerId)) {
            BlockPos lastPos = lastPositions.get(playerId);
            double distance = Math.sqrt(lastPos.getSquaredDistance(currentPos));

            // Only count significant movement (> 1 block)
            if (distance >= 1.0) {
                int blocks = (int) distance;
                DailyChallengeManager.recordProgress(player, ChallengeType.TRAVEL_BLOCKS, blocks);

                // Track distance for Traveler achievement
                com.murilloskills.utils.AchievementTracker.incrementAndCheck(
                        player, com.murilloskills.skills.MurilloSkillsList.EXPLORER,
                        com.murilloskills.utils.AchievementTracker.KEY_DISTANCE_TRAVELED, blocks);

                // Sync less frequently for travel (every 10 blocks)
                if (blocks >= 10) {
                    DailyChallengeManager.syncChallenges(player);
                }

                lastPositions.put(playerId, currentPos);
            }
        } else {
            lastPositions.put(playerId, currentPos);
        }
    }

    /**
     * Tracks biome discovery for explorer challenges.
     */
    private static void trackBiomeDiscovery(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        var world = (net.minecraft.server.world.ServerWorld) player.getEntityWorld();
        var biomeEntry = world.getBiome(player.getBlockPos());
        String biomeId = biomeEntry.getKey().map(k -> k.getValue().toString()).orElse("");

        if (!biomeId.isEmpty()) {
            String lastBiome = lastBiomes.get(playerId);
            if (lastBiome != null && !lastBiome.equals(biomeId)) {
                DailyChallengeManager.recordProgress(player, ChallengeType.DISCOVER_BIOMES, 1);
                DailyChallengeManager.syncChallenges(player);

                // Track biomes for Biome Discoverer achievement
                com.murilloskills.utils.AchievementTracker.incrementAndCheck(
                        player, com.murilloskills.skills.MurilloSkillsList.EXPLORER,
                        com.murilloskills.utils.AchievementTracker.KEY_BIOMES_DISCOVERED, 1);
            }
            lastBiomes.put(playerId, biomeId);
        }
    }

    /**
     * Called by AbstractSkill when ability is used.
     */
    public static void onAbilityUsed(ServerPlayerEntity player) {
        DailyChallengeManager.recordProgress(player, ChallengeType.USE_ABILITY, 1);
        DailyChallengeManager.syncChallenges(player);
    }

    /**
     * Called when player takes damage.
     */
    public static void onDamageTaken(ServerPlayerEntity player, float amount) {
        int damageInt = (int) Math.ceil(amount);
        DailyChallengeManager.recordProgress(player, ChallengeType.TAKE_DAMAGE, damageInt);
        DailyChallengeManager.syncChallenges(player);
    }

    /**
     * Called when player deals damage to entity.
     */
    public static void onDamageDealt(ServerPlayerEntity player, float amount) {
        int damageInt = (int) Math.ceil(amount);
        DailyChallengeManager.recordProgress(player, ChallengeType.DEAL_DAMAGE, damageInt);
        DailyChallengeManager.syncChallenges(player);
    }

    /**
     * Called when player eats food.
     */
    public static void onFoodEaten(ServerPlayerEntity player) {
        DailyChallengeManager.recordProgress(player, ChallengeType.EAT_FOOD, 1);
        DailyChallengeManager.syncChallenges(player);
    }

    /**
     * Called when player sleeps through night.
     */
    public static void onSleepComplete(ServerPlayerEntity player) {
        DailyChallengeManager.recordProgress(player, ChallengeType.SLEEP_NIGHTS, 1);
        DailyChallengeManager.syncChallenges(player);
    }

    /**
     * Called when arrow hits target.
     */
    public static void onArrowHit(ServerPlayerEntity player, double distance, boolean isHeadshot) {
        DailyChallengeManager.recordProgress(player, ChallengeType.ARROW_HITS, 1);

        if (distance >= 30.0) {
            DailyChallengeManager.recordProgress(player, ChallengeType.LONG_SHOTS, 1);
        }

        if (isHeadshot) {
            DailyChallengeManager.recordProgress(player, ChallengeType.HEADSHOTS, 1);
        }

        DailyChallengeManager.syncChallenges(player);
    }

    /**
     * Called when player catches something fishing.
     */
    public static void onFishCaught(ServerPlayerEntity player, boolean isFish, boolean isTreasure) {
        if (isFish) {
            DailyChallengeManager.recordProgress(player, ChallengeType.CATCH_FISH, 1);
        } else if (isTreasure) {
            DailyChallengeManager.recordProgress(player, ChallengeType.CATCH_TREASURE, 1);
        } else {
            DailyChallengeManager.recordProgress(player, ChallengeType.CATCH_JUNK, 1);
        }
        DailyChallengeManager.syncChallenges(player);
    }

    /**
     * Called when player crafts item.
     */
    public static void onItemCrafted(ServerPlayerEntity player, int amount) {
        DailyChallengeManager.recordProgress(player, ChallengeType.CRAFT_ITEMS, amount);
        DailyChallengeManager.syncChallenges(player);
    }

    /**
     * Called when player smelts item.
     */
    public static void onItemSmelted(ServerPlayerEntity player, int amount) {
        DailyChallengeManager.recordProgress(player, ChallengeType.SMELT_ITEMS, amount);
        DailyChallengeManager.syncChallenges(player);
    }

    /**
     * Called when player enchants item.
     */
    public static void onItemEnchanted(ServerPlayerEntity player) {
        DailyChallengeManager.recordProgress(player, ChallengeType.ENCHANT_ITEMS, 1);
        DailyChallengeManager.syncChallenges(player);
    }

    /**
     * Called when player plants seeds.
     */
    public static void onSeedsPlanted(ServerPlayerEntity player) {
        DailyChallengeManager.recordProgress(player, ChallengeType.PLANT_SEEDS, 1);
        DailyChallengeManager.syncChallenges(player);
    }

    /**
     * Called when player breeds animals.
     */
    public static void onAnimalBred(ServerPlayerEntity player) {
        DailyChallengeManager.recordProgress(player, ChallengeType.BREED_ANIMALS, 1);
        DailyChallengeManager.syncChallenges(player);
    }

    /**
     * Called when player shears sheep.
     */
    public static void onSheepSheared(ServerPlayerEntity player) {
        DailyChallengeManager.recordProgress(player, ChallengeType.SHEAR_SHEEP, 1);
        DailyChallengeManager.syncChallenges(player);
    }

    /**
     * Called when player repairs an item (on anvil).
     */
    public static void onItemRepaired(ServerPlayerEntity player) {
        DailyChallengeManager.recordProgress(player, ChallengeType.REPAIR_ITEMS, 1);
        DailyChallengeManager.syncChallenges(player);
    }
}
