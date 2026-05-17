package com.murilloskills.network.handlers;

import com.murilloskills.network.ParagonActivationC2SPayload;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles paragon skill activation at level 99.
 * Validates level requirements and selected skill status.
 */
public final class ParagonActivationNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-ParagonHandler");

    private ParagonActivationNetworkHandler() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a handler for paragon activation requests.
     * 
     * @return ServerPlayNetworking handler for ParagonActivationC2SPayload
     */
    public static ServerPlayNetworking.PlayPayloadHandler<ParagonActivationC2SPayload> create() {
        return (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    var data = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
                    var requestedSkill = payload.skill();
                    data.normalizeParagonState();
                    LOGGER.info("[Paragon] request player={} skill={} selected={} paragonSkills={} active={}",
                            player.getName().getString(), requestedSkill, data.getSelectedSkills(), data.paragonSkills,
                            data.getActiveParagonSkill());

                    if (data.isParagonSkill(requestedSkill)) {
                        LOGGER.warn("[Paragon] rejected player={} skill={} reason=already_paragon",
                                player.getName().getString(), requestedSkill);
                        player.sendMessage(
                                Text.translatable("murilloskills.paragon.already_chosen").formatted(Formatting.RED),
                                true);
                        return;
                    }

                    // Validation: Paragon can only be activated on selected skills
                    if (!data.isSkillSelected(requestedSkill)) {
                        LOGGER.warn("[Paragon] rejected player={} skill={} reason=not_selected selected={}",
                                player.getName().getString(), requestedSkill, data.getSelectedSkills());
                        player.sendMessage(
                                Text.translatable("murilloskills.paragon.only_selected_skills")
                                        .formatted(Formatting.RED),
                                true);
                        return;
                    }

                    if (!data.canActivateParagonSkill(requestedSkill)) {
                        LOGGER.warn("[Paragon] rejected player={} skill={} reason=activation_rule paragonSkills={}",
                                player.getName().getString(), requestedSkill, data.paragonSkills);
                        String messageKey = requestedSkill.isMasterClass()
                                ? "murilloskills.paragon.master_already_chosen"
                                : "murilloskills.paragon.already_chosen";
                        player.sendMessage(Text.translatable(messageKey).formatted(Formatting.RED), true);
                        return;
                    }

                    var stats = data.getSkill(requestedSkill);
                    // Paragon can be selected at level 99 (locks at 99 until chosen)
                    if (stats.level >= 99) {
                        data.activateParagonSkill(requestedSkill);
                        LOGGER.info("[Paragon] activated player={} skill={} level={} paragonSkills={}",
                                player.getName().getString(), requestedSkill, stats.level, data.paragonSkills);

                        SkillsNetworkUtils.syncSkills(player);
                        player.sendMessage(Text.translatable("murilloskills.paragon.defined", requestedSkill.name())
                                .formatted(Formatting.GOLD, Formatting.BOLD), false);

                        // Grant "First Paragon" advancement
                        com.murilloskills.utils.AdvancementGranter.grantFirstParagon(player);
                    } else {
                        LOGGER.warn("[Paragon] rejected player={} skill={} reason=level_insufficient level={}",
                                player.getName().getString(), requestedSkill, stats.level);
                        player.sendMessage(
                                Text.translatable("murilloskills.paragon.level_insufficient").formatted(Formatting.RED),
                                true);
                    }

                } catch (Exception e) {
                    LOGGER.error("Error processing paragon activation", e);
                }
            });
        };
    }
}
