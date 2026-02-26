package com.murilloskills.network.handlers;

import com.murilloskills.network.UltminePreviewS2CPayload;
import com.murilloskills.network.UltmineRequestC2SPayload;
import com.murilloskills.skills.VeinMinerHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles client preview requests for Ultmine.
 */
public final class UltmineRequestNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-UltminePreview");

    private UltmineRequestNetworkHandler() {
    }

    public static ServerPlayNetworking.PlayPayloadHandler<UltmineRequestC2SPayload> create() {
        return (payload, context) -> context.server().execute(() -> {
            try {
                var player = context.player();
                var world = player.getEntityWorld();

                if (!VeinMinerHandler.isVeinMinerActive(player)) {
                    ServerPlayNetworking.send(player, new UltminePreviewS2CPayload(java.util.List.of()));
                    return;
                }

                var state = world.getBlockState(payload.targetPos());
                if (state.isAir()) {
                    ServerPlayNetworking.send(player, new UltminePreviewS2CPayload(java.util.List.of()));
                    return;
                }

                var preview = VeinMinerHandler.getValidatedUltminePreview(player, world, payload.targetPos());
                ServerPlayNetworking.send(player, new UltminePreviewS2CPayload(preview));
            } catch (Exception e) {
                LOGGER.error("Failed to process ultmine preview request", e);
            }
        });
    }
}
