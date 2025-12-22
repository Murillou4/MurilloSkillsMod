package com.murilloskills.network.handlers;

import com.murilloskills.network.PrestigeC2SPayload;
import com.murilloskills.skills.MurilloSkillsList;
import com.murilloskills.utils.PrestigeManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles skill prestige operations.
 * Validates prestige requirements and executes prestige via PrestigeManager.
 */
public final class PrestigeNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-PrestigeHandler");

    private PrestigeNetworkHandler() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a handler for prestige requests.
     * 
     * @return ServerPlayNetworking handler for PrestigeC2SPayload
     */
    public static ServerPlayNetworking.PlayPayloadHandler<PrestigeC2SPayload> create() {
        return (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    var data = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

                    MurilloSkillsList skill = payload.skill();

                    // Validation: Prestige only works on the player's Paragon skill
                    if (data.paragonSkill != skill) {
                        player.sendMessage(
                                Text.translatable("murilloskills.prestige.not_paragon")
                                        .formatted(Formatting.RED),
                                true);
                        return;
                    }

                    // Validation: Check if prestige is possible
                    if (!PrestigeManager.canPrestige(player, skill)) {
                        var stats = data.getSkill(skill);
                        if (stats.prestige >= com.murilloskills.utils.SkillConfig.getMaxPrestigeLevel()) {
                            player.sendMessage(
                                    Text.translatable("murilloskills.prestige.max_reached")
                                            .formatted(Formatting.RED),
                                    true);
                        } else {
                            player.sendMessage(
                                    Text.translatable("murilloskills.paragon.level_insufficient")
                                            .formatted(Formatting.RED),
                                    true);
                        }
                        return;
                    }

                    // Execute prestige
                    if (PrestigeManager.doPrestige(player, skill)) {
                        player.sendMessage(
                                Text.translatable("murilloskills.prestige.success")
                                        .formatted(Formatting.GREEN, Formatting.BOLD),
                                false);
                        LOGGER.info("Player {} prestiged skill {} to level {}",
                                player.getName().getString(), skill.name(), data.getSkill(skill).prestige);

                        // Grant "First Prestige" advancement
                        com.murilloskills.utils.AdvancementGranter.grantFirstPrestige(player);
                    }

                } catch (Exception e) {
                    LOGGER.error("Error processing prestige request", e);
                }
            });
        };
    }
}
