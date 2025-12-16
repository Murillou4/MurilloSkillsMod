package com.murilloskills.network.handlers;

import com.murilloskills.data.SkillGlobalState;
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
                    SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
                    var data = state.getPlayerData(player);

                    if (data.paragonSkill != null) {
                        player.sendMessage(
                                Text.translatable("murilloskills.paragon.already_chosen").formatted(Formatting.RED),
                                true);
                        return;
                    }

                    // Validation: Paragon can only be activated on selected skills
                    if (!data.isSkillSelected(payload.skill())) {
                        player.sendMessage(
                                Text.translatable("murilloskills.paragon.only_selected_skills")
                                        .formatted(Formatting.RED),
                                true);
                        return;
                    }

                    var stats = data.getSkill(payload.skill());
                    // Paragon can be selected at level 99 (locks at 99 until chosen)
                    if (stats.level >= 99) {
                        data.paragonSkill = payload.skill();
                        state.markDirty();
                        SkillsNetworkUtils.syncSkills(player);
                        player.sendMessage(Text.translatable("murilloskills.paragon.defined", payload.skill().name())
                                .formatted(Formatting.GOLD, Formatting.BOLD), false);
                    } else {
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
