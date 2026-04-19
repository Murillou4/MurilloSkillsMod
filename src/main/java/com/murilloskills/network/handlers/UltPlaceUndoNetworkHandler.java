package com.murilloskills.network.handlers;

import com.murilloskills.network.UltPlaceUndoC2SPayload;
import com.murilloskills.skills.UltPlaceHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UltPlaceUndoNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-UltPlaceUndo");

    private UltPlaceUndoNetworkHandler() {
    }

    public static ServerPlayNetworking.PlayPayloadHandler<UltPlaceUndoC2SPayload> create() {
        return (payload, context) -> context.server().execute(() -> {
            try {
                UltPlaceHandler.undoLast(context.player());
            } catch (Exception e) {
                LOGGER.error("Failed to undo UltPlace placement", e);
            }
        });
    }
}
