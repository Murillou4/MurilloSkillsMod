package com.murilloskills.network.handlers;

import com.murilloskills.api.SkillRegistry;

import com.murilloskills.impl.ExplorerSkill;
import com.murilloskills.network.NightVisionToggleC2SPayload;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles toggle for Night Vision effect (Explorer skill, Level 35+).
 * Validates level requirements and delegates to ExplorerSkill.
 */
public final class NightVisionToggleNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-NightVisionHandler");

    private NightVisionToggleNetworkHandler() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a handler for night vision toggle requests.
     * 
     * @return ServerPlayNetworking handler for NightVisionToggleC2SPayload
     */
    public static ServerPlayNetworking.PlayPayloadHandler<NightVisionToggleC2SPayload> create() {
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

                    // Check level requirement (Level 35 for Night Vision)
                    if (explorerStats.level < SkillConfig.EXPLORER_NIGHT_VISION_LEVEL) {
                        player.sendMessage(
                                Text.translatable("murilloskills.explorer.need_level_35_night_vision")
                                        .formatted(Formatting.RED),
                                true);
                        return;
                    }

                    // Toggle Night Vision using the ExplorerSkill method
                    ExplorerSkill explorerSkill = (ExplorerSkill) SkillRegistry.get(MurilloSkillsList.EXPLORER);
                    if (explorerSkill != null) {
                        explorerSkill.toggleNightVision(player);
                    }

                } catch (Exception e) {
                    LOGGER.error("Error processing night vision toggle", e);
                }
            });
        };
    }
}
