package com.murilloskills.network.handlers;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.impl.BuilderSkill;
import com.murilloskills.network.HollowFillToggleC2SPayload;
import com.murilloskills.skills.MurilloSkillsList;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles toggle for hollow/filled building mode (Builder skill).
 * Validates Builder skill selection and toggles build mode.
 */
public final class HollowFillNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-HollowFillHandler");

    private HollowFillNetworkHandler() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a handler for hollow/fill toggle requests.
     * 
     * @return ServerPlayNetworking handler for HollowFillToggleC2SPayload
     */
    public static ServerPlayNetworking.PlayPayloadHandler<HollowFillToggleC2SPayload> create() {
        return (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
                    var playerData = state.getPlayerData(player);

                    // Check if player has BUILDER selected
                    if (!playerData.isSkillSelected(MurilloSkillsList.BUILDER)) {
                        player.sendMessage(
                                Text.translatable("murilloskills.builder.need_builder_skill")
                                        .formatted(Formatting.RED),
                                true);
                        return;
                    }

                    // Toggle hollow mode
                    boolean nowHollow = BuilderSkill.toggleHollowMode(player);

                    // Feedback message
                    if (nowHollow) {
                        player.sendMessage(Text.translatable("murilloskills.builder.mode_hollow")
                                .formatted(Formatting.AQUA), true);
                    } else {
                        player.sendMessage(Text.translatable("murilloskills.builder.mode_filled")
                                .formatted(Formatting.GREEN), true);
                    }

                } catch (Exception e) {
                    LOGGER.error("Error processing hollow/fill toggle", e);
                }
            });
        };
    }
}
