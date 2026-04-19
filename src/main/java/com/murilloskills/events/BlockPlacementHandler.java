package com.murilloskills.events;

import com.murilloskills.impl.BuilderSkill;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.skills.UltPlaceHandler;
import com.murilloskills.utils.BuilderXpGetter;
import com.murilloskills.utils.SkillConfig;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles Builder XP and side effects after a block has been placed successfully.
 */
public final class BlockPlacementHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-BlockPlacement");

    private BlockPlacementHandler() {
    }

    public static void register() {
        LOGGER.info("BlockPlacementHandler ready - Builder XP now uses successful post-placement hooks");
    }

    public static void onBlockPlaced(ServerPlayerEntity serverPlayer, ServerWorld world, BlockPos placementPos, Block block,
            Direction face, Hand hand, net.minecraft.item.ItemStack sourceStack, Vec3d hitPos,
            Map<BlockPos, BlockState> previousStates) {
        var playerData = serverPlayer.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
        if (!playerData.isSkillSelected(MurilloSkillsList.BUILDER)) {
            return;
        }
        if (UltPlaceHandler.isSyntheticPlacementActive(serverPlayer)) {
            return;
        }

        LOGGER.debug("Builder placed block: {} at {}", block.getName().getString(), placementPos);

        try {
            var xpResult = BuilderXpGetter.getBlockPlacementXp(block);
            if (xpResult.didGainXp()) {
                awardPlacementXp(serverPlayer, placementPos, block, playerData, xpResult.getXpAmount());
            }

            if (BuilderSkill.isCreativeBrushActive(serverPlayer)) {
                int extraBlocks = BuilderSkill.handleCreativeBrushPlacement(serverPlayer, world, placementPos, block);
                if (extraBlocks > 0 && xpResult.didGainXp()) {
                    awardCreativeBrushXp(serverPlayer, playerData, xpResult.getXpAmount(), extraBlocks);
                }
            }

            UltPlaceHandler.handle(serverPlayer, world, placementPos, face, hand, sourceStack, hitPos, previousStates);
        } catch (Exception e) {
            LOGGER.error("Error processing Builder XP for block placement", e);
        }
    }

    private static void awardPlacementXp(ServerPlayerEntity serverPlayer, BlockPos placementPos, Block block,
            com.murilloskills.data.PlayerSkillData playerData, int xpAmount) {
        var builderStats = playerData.getSkill(MurilloSkillsList.BUILDER);
        com.murilloskills.data.PlayerSkillData.XpAddResult xpAddResult = playerData.addXpToSkill(
                MurilloSkillsList.BUILDER,
                xpAmount);

        com.murilloskills.utils.VanillaXpRewarder.checkAndRewardMilestone(serverPlayer, "Construtor", xpAddResult);

        if (xpAddResult.leveledUp()) {
            SkillNotifier.notifyLevelUp(serverPlayer, MurilloSkillsList.BUILDER, builderStats.level);
            com.murilloskills.utils.SkillAttributes.updateAllStats(serverPlayer);
        }

        SkillsNetworkUtils.syncSkills(serverPlayer);

        LOGGER.debug("Awarded {} Builder XP to {} for placing {}",
                xpAmount, serverPlayer.getName().getString(), block.getName().getString());

        com.murilloskills.utils.DailyChallengeManager.recordProgress(serverPlayer,
                com.murilloskills.utils.DailyChallengeManager.ChallengeType.PLACE_BLOCKS, 1);

        if (placementPos.getY() > SkillConfig.BUILDER_HIGH_BUILD_Y_THRESHOLD) {
            com.murilloskills.utils.DailyChallengeManager.recordProgress(serverPlayer,
                    com.murilloskills.utils.DailyChallengeManager.ChallengeType.BUILD_HEIGHT, 1);
        }

        String blockId = net.minecraft.registry.Registries.BLOCK.getId(block).toString();
        if (blockId.contains("stairs")) {
            com.murilloskills.utils.DailyChallengeManager.recordProgress(serverPlayer,
                    com.murilloskills.utils.DailyChallengeManager.ChallengeType.PLACE_STAIRS, 1);
        }

        com.murilloskills.utils.DailyChallengeManager.syncChallenges(serverPlayer);

        com.murilloskills.utils.AchievementTracker.incrementAndCheck(
                serverPlayer, MurilloSkillsList.BUILDER,
                com.murilloskills.utils.AchievementTracker.KEY_BLOCKS_PLACED, 1);

        if (placementPos.getY() > 200) {
            com.murilloskills.utils.AdvancementGranter.grantHeightMaster(serverPlayer);
        }
    }

    private static void awardCreativeBrushXp(ServerPlayerEntity serverPlayer,
            com.murilloskills.data.PlayerSkillData playerData, int xpPerBlock, int extraBlocks) {
        com.murilloskills.data.PlayerSkillData.XpAddResult brushResult = playerData.addXpToSkill(
                MurilloSkillsList.BUILDER,
                xpPerBlock * extraBlocks);

        com.murilloskills.utils.VanillaXpRewarder.checkAndRewardMilestone(serverPlayer, "Construtor", brushResult);

        if (brushResult.leveledUp()) {
            var builderStats = playerData.getSkill(MurilloSkillsList.BUILDER);
            SkillNotifier.notifyLevelUp(serverPlayer, MurilloSkillsList.BUILDER, builderStats.level);
        }

        SkillsNetworkUtils.syncSkills(serverPlayer);

        LOGGER.debug("Creative Brush placed {} extra blocks for {}",
                extraBlocks, serverPlayer.getName().getString());
    }
}
