package com.murilloskills.network.handlers;

import com.murilloskills.network.StorageWhitelistSyncC2SPayload;
import com.murilloskills.skills.VeinMinerHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persists the player's storage routing whitelist on the server.
 */
public final class StorageWhitelistSyncNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-StorageWhitelistHandler");

    private StorageWhitelistSyncNetworkHandler() {
    }

    public static ServerPlayNetworking.PlayPayloadHandler<StorageWhitelistSyncC2SPayload> create() {
        return (payload, context) -> {
            context.server().execute(() -> {
                try {
                    VeinMinerHandler.setStorageWhitelist(context.player(), payload.items());
                } catch (Exception e) {
                    LOGGER.error("Error processing storage whitelist sync", e);
                }
            });
        };
    }
}
