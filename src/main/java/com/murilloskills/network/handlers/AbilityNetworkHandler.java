package com.murilloskills.network.handlers;

import com.murilloskills.api.AbstractSkill;
import com.murilloskills.api.SkillRegistry;

import com.murilloskills.network.SkillAbilityC2SPayload;
import com.murilloskills.utils.SkillsNetworkUtils;
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
                    var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
                    var requestedParagon = payload.skill();
                    var activeParagon = requestedParagon != null ? requestedParagon : playerData.getActiveParagonSkill();
                    LOGGER.info("[Ability] request player={} requested={} resolved={} paragonSkills={} selectedSkills={}",
                            player.getName().getString(), requestedParagon, activeParagon, playerData.paragonSkills,
                            playerData.selectedSkills);

                    if (activeParagon == null) {
                        LOGGER.warn("[Ability] rejected player={} reason=no_active_paragon",
                                player.getName().getString());
                        player.sendMessage(Text.translatable("murilloskills.paragon.need_confirm")
                                .formatted(Formatting.RED), true);
                        return;
                    }

                    if (!playerData.isParagonSkill(activeParagon)) {
                        LOGGER.warn("[Ability] rejected player={} skill={} reason=not_paragon paragonSkills={}",
                                player.getName().getString(), activeParagon, playerData.paragonSkills);
                        player.sendMessage(Text.translatable("murilloskills.prestige.not_paragon")
                                .formatted(Formatting.RED), true);
                        return;
                    }

                    // Clean polymorphic approach using SkillRegistry
                    AbstractSkill skill = SkillRegistry.get(activeParagon);
                    if (skill != null) {
                        var stats = playerData.getSkill(activeParagon);
                        long before = stats.lastAbilityUse;
                        LOGGER.info("[Ability] activating player={} skill={} level={} prestige={} lastUseBefore={}",
                                player.getName().getString(), activeParagon, stats.level, stats.prestige, before);
                        skill.onActiveAbility(player, stats);
                        LOGGER.info("[Ability] completed player={} skill={} lastUseAfter={}",
                                player.getName().getString(), activeParagon, stats.lastAbilityUse);
                        SkillsNetworkUtils.syncSkills(player);
                    } else {
                        LOGGER.warn("Paragon skill not found in registry: {}", activeParagon);
                        player.sendMessage(Text
                                .translatable("murilloskills.paragon.in_development", activeParagon.name())
                                .formatted(Formatting.YELLOW), true);
                    }

                } catch (Exception e) {
                    LOGGER.error("Error processing ability activation", e);
                }
            });
        };
    }
}
