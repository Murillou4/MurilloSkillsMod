package com.murilloskills.network.handlers;

import com.murilloskills.network.VeinMinerToggleC2SPayload;
import com.murilloskills.skills.VeinMinerHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles vein miner key press/release (hold to activate).
 */
public final class VeinMinerToggleNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-VeinMinerHandler");

    private VeinMinerToggleNetworkHandler() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a handler for vein miner key state changes.
     *
     * @return ServerPlayNetworking handler for VeinMinerToggleC2SPayload
     */
    public static ServerPlayNetworking.PlayPayloadHandler<VeinMinerToggleC2SPayload> create() {
        return (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    VeinMinerHandler.setVeinMinerActive(player, payload.activated());
                } catch (Exception e) {
                    LOGGER.error("Error processing vein miner state", e);
                }
            });
        };
    }
}
