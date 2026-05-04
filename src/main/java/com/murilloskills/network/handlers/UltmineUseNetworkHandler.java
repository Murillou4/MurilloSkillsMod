package com.murilloskills.network.handlers;

import com.murilloskills.network.UltmineUseC2SPayload;
import com.murilloskills.skills.UltmineUseHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles explicit Ultmine right-click action requests.
 */
public final class UltmineUseNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-UltmineUse");

    private UltmineUseNetworkHandler() {
    }

    public static ServerPlayNetworking.PlayPayloadHandler<UltmineUseC2SPayload> create() {
        return (payload, context) -> context.server().execute(() -> {
            try {
                UltmineUseHandler.handleUseRequest(context.player(), payload.targetPos(), payload.face(),
                        payload.hand(), payload.hitPos(), payload.shape(), payload.depth(), payload.length(),
                        payload.variant());
            } catch (Exception e) {
                LOGGER.error("Failed to process ultmine right-click use", e);
            }
        });
    }
}
