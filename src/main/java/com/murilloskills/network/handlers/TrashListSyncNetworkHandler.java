package com.murilloskills.network.handlers;

import com.murilloskills.network.TrashListSyncC2SPayload;
import com.murilloskills.skills.VeinMinerHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles trash list sync from client.
 */
public final class TrashListSyncNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-TrashListHandler");

    private TrashListSyncNetworkHandler() {
    }

    public static ServerPlayNetworking.PlayPayloadHandler<TrashListSyncC2SPayload> create() {
        return (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    VeinMinerHandler.setTrashList(player, payload.trashItems());
                } catch (Exception e) {
                    LOGGER.error("Error processing trash list sync", e);
                }
            });
        };
    }
}
