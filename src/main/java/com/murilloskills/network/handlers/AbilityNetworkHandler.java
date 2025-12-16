package com.murilloskills.network.handlers;

import com.murilloskills.api.AbstractSkill;
import com.murilloskills.api.SkillRegistry;
import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.network.SkillAbilityC2SPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles active ability activation requests (Z key).
 * Validates paragon skill selection and delegates execution to the skill
 * implementation.
 */
public final class AbilityNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-AbilityHandler");

    private AbilityNetworkHandler() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a handler for skill ability activation.
     * 
     * @return ServerPlayNetworking handler for SkillAbilityC2SPayload
     */
    public static ServerPlayNetworking.PlayPayloadHandler<SkillAbilityC2SPayload> create() {
        return (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
                    var playerData = state.getPlayerData(player);

                    if (playerData.paragonSkill == null) {
                        player.sendMessage(Text.translatable("murilloskills.paragon.need_confirm")
                                .formatted(Formatting.RED), true);
                        return;
                    }

                    // Clean polymorphic approach using SkillRegistry
                    AbstractSkill skill = SkillRegistry.get(playerData.paragonSkill);
                    if (skill != null) {
                        var stats = playerData.getSkill(playerData.paragonSkill);
                        skill.onActiveAbility(player, stats);
                    } else {
                        LOGGER.warn("Paragon skill not found in registry: {}", playerData.paragonSkill);
                        player.sendMessage(Text
                                .translatable("murilloskills.paragon.in_development", playerData.paragonSkill.name())
                                .formatted(Formatting.YELLOW), true);
                    }

                } catch (Exception e) {
                    LOGGER.error("Error processing ability activation", e);
                }
            });
        };
    }
}
