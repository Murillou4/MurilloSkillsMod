package com.murilloskills.network.handlers;

import com.murilloskills.api.SkillRegistry;

import com.murilloskills.impl.ExplorerSkill;
import com.murilloskills.network.StepAssistToggleC2SPayload;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.SkillConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles toggle for Step Assist effect (Explorer skill, Level 10+).
 * Validates level requirements and delegates to ExplorerSkill.
 */
public final class StepAssistToggleNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-StepAssistHandler");

    private StepAssistToggleNetworkHandler() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a handler for step assist toggle requests.
     * 
     * @return ServerPlayNetworking handler for StepAssistToggleC2SPayload
     */
    public static ServerPlayNetworking.PlayPayloadHandler<StepAssistToggleC2SPayload> create() {
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

                    // Check level requirement (Level 10 for Step Assist)
                    if (explorerStats.level < SkillConfig.EXPLORER_STEP_ASSIST_LEVEL) {
                        player.sendMessage(
                                Text.translatable("murilloskills.explorer.need_level_35")
                                        .formatted(Formatting.RED),
                                true);
                        return;
                    }

                    // Toggle Step Assist using the ExplorerSkill method
                    ExplorerSkill explorerSkill = (ExplorerSkill) SkillRegistry.get(MurilloSkillsList.EXPLORER);
                    if (explorerSkill != null) {
                        explorerSkill.toggleStepAssist(player);
                    }

                } catch (Exception e) {
                    LOGGER.error("Error processing step assist toggle", e);
                }
            });
        };
    }
}
