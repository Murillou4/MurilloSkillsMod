package com.murilloskills.skills;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.impl.FisherSkill;
import com.murilloskills.models.SkillReceptorResult;
import com.murilloskills.utils.EpicBundleGenerator;
import com.murilloskills.utils.FisherXpGetter;
import com.murilloskills.utils.SkillConfig;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Handler for fishing catch events.
 * Awards XP based on item category and handles Epic Bundle generation.
 */
public class FishingCatchHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-FishingCatch");
    private static final Random random = new Random();

    /**
     * Handles a fishing catch event.
     * 
     * @param player      The player who caught the item
     * @param caughtStack The item that was caught
     */
    public static void handle(PlayerEntity player, ItemStack caughtStack) {
        if (player.getEntityWorld().isClient()) {
            return;
        }

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        try {
            SkillGlobalState state = SkillGlobalState.getServerState(serverPlayer.getEntityWorld().getServer());
            var playerData = state.getPlayerData(serverPlayer);

            // Check if player has Fisher as their selected skill
            if (!playerData.isSkillSelected(MurilloSkillsList.FISHER)) {
                return;
            }

            var fisherStats = playerData.getSkill(MurilloSkillsList.FISHER);
            int level = fisherStats.level;

            // Calculate XP
            Item caughtItem = caughtStack.getItem();
            SkillReceptorResult xpResult = FisherXpGetter.getFishingXp(caughtItem);

            if (xpResult.didGainXp()) {
                int xpAmount = xpResult.getXpAmount();

                // Level 25: +10% XP bonus
                if (level >= SkillConfig.FISHER_TREASURE_BONUS_LEVEL) {
                    xpAmount = (int) (xpAmount * (1.0f + SkillConfig.FISHER_XP_BONUS));
                }

                // Add XP using centralized logic (handles constraints and level up)
                boolean leveledUp = playerData.addXpToSkill(MurilloSkillsList.FISHER, xpAmount);

                if (leveledUp) {
                    SkillNotifier.notifyLevelUp(serverPlayer, MurilloSkillsList.FISHER, fisherStats.level);
                }

                state.markDirty();
                SkillsNetworkUtils.syncSkills(serverPlayer); // Sync with client

                // Notify player (action bar message)
                String category = FisherXpGetter.getCategoryName(caughtItem);
                serverPlayer.sendMessage(
                        Text.literal("+" + xpAmount + " XP Fisher (" + category + ")")
                                .formatted(Formatting.AQUA),
                        true);

                LOGGER.debug("Player {} pescou {} ({}) - +{} XP Fisher",
                        serverPlayer.getName().getString(),
                        caughtItem.getName().getString(),
                        category,
                        xpAmount);
            }

            // Check for Epic Bundle
            handleEpicBundle(serverPlayer, level, (ServerWorld) serverPlayer.getEntityWorld());

        } catch (Exception e) {
            LOGGER.error("Erro ao processar pescaria para " + player.getName().getString(), e);
        }
    }

    /**
     * Handles Epic Bundle generation based on player level.
     */
    private static void handleEpicBundle(ServerPlayerEntity player, int level, ServerWorld world) {
        // Calculate Epic Bundle chance
        float baseChance = level * SkillConfig.FISHER_EPIC_BUNDLE_PER_LEVEL;

        // Check if Rain Dance is active for triple chance
        if (FisherSkill.isRainDanceActive(player)) {
            baseChance *= SkillConfig.FISHER_RAIN_DANCE_BUNDLE_MULTIPLIER;
        }

        // Cap at 90% during Rain Dance (30% * 3)
        baseChance = Math.min(baseChance, 0.90f);

        // Roll for Epic Bundle
        if (random.nextFloat() < baseChance) {
            EpicBundleGenerator.generateAndSpawn(player, world);

            player.sendMessage(
                    Text.literal("✦ BUNDLE ÉPICO! ✦")
                            .formatted(Formatting.GOLD, Formatting.BOLD),
                    false);

            LOGGER.info("Player {} pescou um Bundle Épico!", player.getName().getString());
        }
    }

}
