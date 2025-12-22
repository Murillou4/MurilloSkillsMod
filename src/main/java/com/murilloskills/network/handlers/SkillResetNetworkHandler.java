package com.murilloskills.network.handlers;

import com.murilloskills.network.SkillResetC2SPayload;
import com.murilloskills.utils.SkillAttributes;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles skill reset operations (level and XP back to 0).
 * Validates selected skills and updates player attributes.
 */
public final class SkillResetNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-ResetHandler");

    private SkillResetNetworkHandler() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a handler for skill reset requests.
     * 
     * @return ServerPlayNetworking handler for SkillResetC2SPayload
     */
    public static ServerPlayNetworking.PlayPayloadHandler<SkillResetC2SPayload> create() {
        return (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    var data = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

                    // Validation: Can only reset selected skills
                    if (!data.isSkillSelected(payload.skill())) {
                        player.sendMessage(Text.translatable("murilloskills.reset.only_selected")
                                .formatted(Formatting.RED), true);
                        return;
                    }

                    // Reset the skill to level 0 and XP 0
                    var stats = data.getSkill(payload.skill());
                    stats.level = 0;
                    stats.xp = 0;
                    stats.lastAbilityUse = -1; // Reset cooldown too

                    // If this was the paragon skill, remove paragon status
                    if (data.paragonSkill == payload.skill()) {
                        data.paragonSkill = null;
                    }

                    // Remove skill from selection - player can now choose a new one
                    data.selectedSkills.remove(payload.skill());

                    // Update attributes to reflect reset
                    SkillAttributes.updateAllStats(player);

                    SkillsNetworkUtils.syncSkills(player);

                    player.sendMessage(
                            Text.translatable("murilloskills.reset.success", payload.skill().name())
                                    .formatted(Formatting.YELLOW),
                            false);

                    LOGGER.info("Player {} reset skill {} to level 0",
                            player.getName().getString(), payload.skill().name());

                } catch (Exception e) {
                    LOGGER.error("Error processing skill reset", e);
                }
            });
        };
    }
}
