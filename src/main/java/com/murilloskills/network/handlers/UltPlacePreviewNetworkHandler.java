package com.murilloskills.network.handlers;

import com.murilloskills.network.UltPlacePreviewRequestC2SPayload;
import com.murilloskills.network.UltPlacePreviewS2CPayload;
import com.murilloskills.skills.UltPlaceHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class UltPlacePreviewNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-UltPlacePreview");

    private UltPlacePreviewNetworkHandler() {
    }

    public static ServerPlayNetworking.PlayPayloadHandler<UltPlacePreviewRequestC2SPayload> create() {
        return (payload, context) -> context.server().execute(() -> {
            try {
                var player = context.player();
                var world = player.getEntityWorld();

                if (!UltPlaceHandler.isEnabled(player)) {
                    ServerPlayNetworking.send(player, new UltPlacePreviewS2CPayload(payload.requestKey(), List.of()));
                    return;
                }

                if (player.getEyePos().squaredDistanceTo(payload.targetPos().toCenterPos()) > 81.0) {
                    ServerPlayNetworking.send(player, new UltPlacePreviewS2CPayload(payload.requestKey(), List.of()));
                    return;
                }

                var preview = UltPlaceHandler.getValidatedPreview(player, world, payload.targetPos(), payload.face(),
                        payload.hitPos());
                ServerPlayNetworking.send(player, new UltPlacePreviewS2CPayload(payload.requestKey(), preview));
            } catch (Exception e) {
                LOGGER.error("Failed to process UltPlace preview request", e);
            }
        });
    }
}
