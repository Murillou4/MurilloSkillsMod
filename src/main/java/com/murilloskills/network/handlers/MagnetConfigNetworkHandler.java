package com.murilloskills.network.handlers;

import com.murilloskills.network.MagnetConfigC2SPayload;
import com.murilloskills.skills.VeinMinerHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles magnet configuration updates from client.
 */
public final class MagnetConfigNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-MagnetHandler");

    private MagnetConfigNetworkHandler() {
    }

    public static ServerPlayNetworking.PlayPayloadHandler<MagnetConfigC2SPayload> create() {
        return (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    VeinMinerHandler.setMagnetEnabled(player, payload.enabled());
                    VeinMinerHandler.setMagnetRange(player, payload.range());

                    if (payload.enabled()) {
                        player.sendMessage(Text.translatable("murilloskills.magnet.enabled")
                                .formatted(Formatting.GREEN), true);
                    } else {
                        player.sendMessage(Text.translatable("murilloskills.magnet.disabled")
                                .formatted(Formatting.GRAY), true);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error processing magnet config", e);
                }
            });
        };
    }
}
