package com.murilloskills.events;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.impl.BuilderSkill;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.BuilderXpGetter;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for block placement events to award Builder skill XP.
 * Also handles Creative Brush line placement during active ability.
 */
public class BlockPlacementHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-BlockPlacement");

    /**
     * Registers the block placement event handler.
     * Call this from the main mod initializer.
     */
    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // Only process on server side
            if (world.isClient()) {
                return ActionResult.PASS;
            }

            // Only process for players
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            // Check if player is placing a block
            ItemStack heldStack = player.getStackInHand(hand);
            if (!(heldStack.getItem() instanceof BlockItem blockItem)) {
                return ActionResult.PASS;
            }

            // Check if player has Builder skill selected
            SkillGlobalState state = SkillGlobalState.getServerState(serverPlayer.getEntityWorld().getServer());
            var playerData = state.getPlayerData(serverPlayer);

            if (!playerData.isSkillSelected(MurilloSkillsList.BUILDER)) {
                return ActionResult.PASS;
            }

            // Get the block being placed
            Block block = blockItem.getBlock();
            BlockPos placementPos = hitResult.getBlockPos().offset(hitResult.getSide());

            LOGGER.debug("Builder placing block: {} at {}", block.getName().getString(), placementPos);

            // Award XP for block placement
            var xpResult = BuilderXpGetter.getBlockPlacementXp(block);
            if (xpResult.didGainXp()) {
                var builderStats = playerData.getSkill(MurilloSkillsList.BUILDER);

                // Use proper addXpToSkill method for level-up handling
                SkillGlobalState.XpAddResult xpAddResult = playerData.addXpToSkill(MurilloSkillsList.BUILDER,
                        xpResult.getXpAmount());

                // Check for milestone rewards
                com.murilloskills.utils.VanillaXpRewarder.checkAndRewardMilestone(serverPlayer, "Construtor",
                        xpAddResult);

                if (xpAddResult.leveledUp()) {
                    // Player leveled up!
                    SkillNotifier.notifyLevelUp(serverPlayer, MurilloSkillsList.BUILDER, builderStats.level);
                    // Update attributes with new level bonuses
                    com.murilloskills.utils.SkillAttributes.updateAllStats(serverPlayer);
                }

                state.markDirty();
                SkillsNetworkUtils.syncSkills(serverPlayer);

                LOGGER.debug("Awarded {} XP to {} for placing {}",
                        xpResult.getXpAmount(), serverPlayer.getName().getString(), block.getName().getString());

                // Track daily challenge progress - Builder challenges
                com.murilloskills.utils.DailyChallengeManager.recordProgress(serverPlayer,
                        com.murilloskills.utils.DailyChallengeManager.ChallengeType.PLACE_BLOCKS, 1);

                // Check for high building
                if (placementPos.getY() > 100) {
                    com.murilloskills.utils.DailyChallengeManager.recordProgress(serverPlayer,
                            com.murilloskills.utils.DailyChallengeManager.ChallengeType.BUILD_HEIGHT, 1);
                }

                // Check for stairs
                String blockId = net.minecraft.registry.Registries.BLOCK.getId(block).toString();
                if (blockId.contains("stairs")) {
                    com.murilloskills.utils.DailyChallengeManager.recordProgress(serverPlayer,
                            com.murilloskills.utils.DailyChallengeManager.ChallengeType.PLACE_STAIRS, 1);
                }

                com.murilloskills.utils.DailyChallengeManager.syncChallenges(serverPlayer);
            }

            // Handle Creative Brush line placement
            if (BuilderSkill.isCreativeBrushActive(serverPlayer)) {
                int extraBlocks = BuilderSkill.handleCreativeBrushPlacement(
                        serverPlayer, (ServerWorld) world, placementPos, block);

                if (extraBlocks > 0) {
                    // Award bonus XP for extra blocks placed
                    SkillGlobalState.XpAddResult brushResult = playerData.addXpToSkill(MurilloSkillsList.BUILDER,
                            xpResult.getXpAmount() * extraBlocks);

                    // Check for milestone rewards
                    com.murilloskills.utils.VanillaXpRewarder.checkAndRewardMilestone(serverPlayer, "Construtor",
                            brushResult);

                    if (brushResult.leveledUp()) {
                        var builderStats = playerData.getSkill(MurilloSkillsList.BUILDER);
                        SkillNotifier.notifyLevelUp(serverPlayer, MurilloSkillsList.BUILDER, builderStats.level);
                    }
                    state.markDirty();
                    SkillsNetworkUtils.syncSkills(serverPlayer);

                    LOGGER.debug("Creative Brush placed {} extra blocks for {}",
                            extraBlocks, serverPlayer.getName().getString());
                }
            }

            return ActionResult.PASS;
        });

        LOGGER.info("BlockPlacementHandler registered for Builder skill");
    }
}
