package com.murilloskills.network.handlers;

import com.murilloskills.network.UltmineClassicBlockListSyncC2SPayload;
import com.murilloskills.skills.VeinMinerHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles classic-mode block lock list sync from client.
 */
public final class UltmineClassicBlockListSyncNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-UltmineClassicBlockListHandler");

    private UltmineClassicBlockListSyncNetworkHandler() {
    }

    public static ServerPlayNetworking.PlayPayloadHandler<UltmineClassicBlockListSyncC2SPayload> create() {
        return (payload, context) -> context.server().execute(() -> {
            try {
                var player = context.player();
                VeinMinerHandler.setClassicBlockedBlockList(player, payload.blockIds());
            } catch (Exception e) {
                LOGGER.error("Error processing ultmine classic block list sync", e);
            }
        });
    }
}
