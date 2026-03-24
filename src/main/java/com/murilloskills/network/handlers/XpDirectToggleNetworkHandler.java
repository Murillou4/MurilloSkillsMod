package com.murilloskills.network.handlers;

import com.murilloskills.network.XpDirectToggleC2SPayload;
import com.murilloskills.skills.VeinMinerHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles toggle for XP direct-to-player mode.
 */
public final class XpDirectToggleNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-XpDirectHandler");

    private XpDirectToggleNetworkHandler() {
    }

    public static ServerPlayNetworking.PlayPayloadHandler<XpDirectToggleC2SPayload> create() {
        return (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    VeinMinerHandler.setXpDirectToPlayer(player, payload.enabled());
                    boolean nowEnabled = payload.enabled();

                    if (nowEnabled) {
                        player.sendMessage(Text.translatable("murilloskills.xp_direct.enabled")
                                .formatted(Formatting.GREEN), true);
                    } else {
                        player.sendMessage(Text.translatable("murilloskills.xp_direct.disabled")
                                .formatted(Formatting.GRAY), true);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error processing XP direct toggle", e);
                }
            });
        };
    }
}
