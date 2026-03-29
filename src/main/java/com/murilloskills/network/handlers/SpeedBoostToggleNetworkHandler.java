package com.murilloskills.network.handlers;

import com.murilloskills.api.SkillRegistry;

import com.murilloskills.impl.ExplorerSkill;
import com.murilloskills.network.SpeedBoostToggleC2SPayload;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles toggle for Pathfinder speed boost effect (Explorer skill, Level 45+).
 * Validates level requirements and delegates to ExplorerSkill.
 */
public final class SpeedBoostToggleNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-SpeedBoostHandler");

    private SpeedBoostToggleNetworkHandler() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a handler for speed boost toggle requests.
     *
     * @return ServerPlayNetworking handler for SpeedBoostToggleC2SPayload
     */
    public static ServerPlayNetworking.PlayPayloadHandler<SpeedBoostToggleC2SPayload> create() {
        return (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

                    // Check if player has EXPLORER selected
                    if (!playerData.isSkillSelected(MurilloSkillsList.EXPLORER)) {
                        player.sendMessage(
                                Text.translatable("murilloskills.explorer.need_explorer_skill")
                                        .formatted(Formatting.RED),
                                true);
                        return;
                    }

                    var explorerStats = playerData.getSkill(MurilloSkillsList.EXPLORER);

                    // Check level requirement (Level 45 for Pathfinder)
                    if (explorerStats.level < SkillConfig.EXPLORER_PATHFINDER_LEVEL) {
                        player.sendMessage(
                                Text.translatable("murilloskills.explorer.need_level_45_pathfinder")
                                        .formatted(Formatting.RED),
                                true);
                        return;
                    }

                    // Toggle Speed Boost using the ExplorerSkill method
                    ExplorerSkill explorerSkill = (ExplorerSkill) SkillRegistry.get(MurilloSkillsList.EXPLORER);
                    if (explorerSkill != null) {
                        explorerSkill.toggleSpeedBoost(player);
                    }

                } catch (Exception e) {
                    LOGGER.error("Error processing speed boost toggle", e);
                }
            });
        };
    }
}
