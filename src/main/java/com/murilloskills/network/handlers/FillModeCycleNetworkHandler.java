package com.murilloskills.network.handlers;

import com.murilloskills.network.FillModeCycleC2SPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Network handler for fill mode cycling requests from clients.
 * Delegates to FillModeCycleC2SPayload.handle() for the actual logic.
 */
public class FillModeCycleNetworkHandler {

    /**
     * Creates the handler for fill mode cycle requests
     */
    public static ServerPlayNetworking.PlayPayloadHandler<FillModeCycleC2SPayload> create() {
        return (payload, context) -> {
            context.server().execute(() -> {
                FillModeCycleC2SPayload.handle(payload, context.player());
            });
        };
    }
}
