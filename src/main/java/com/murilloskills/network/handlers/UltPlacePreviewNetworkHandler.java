package com.murilloskills.network.handlers;

import com.murilloskills.network.UltPlacePreviewRequestC2SPayload;
import com.murilloskills.network.UltPlacePreviewS2CPayload;
import com.murilloskills.skills.UltPlaceHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class UltPlacePreviewNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-UltPlacePreview");
    private static final double PREVIEW_RANGE_MARGIN = 1.5D;

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

                if (!isWithinPreviewRange(player, payload.hitPos(), payload.targetPos())) {
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

    private static boolean isWithinPreviewRange(ServerPlayerEntity player, Vec3d hitPos, BlockPos targetPos) {
        Vec3d target = hitPos == null ? targetPos.toCenterPos() : hitPos;
        double maxRange = Math.max(9.0D, player.getBlockInteractionRange() + PREVIEW_RANGE_MARGIN);
        return player.getEyePos().squaredDistanceTo(target) <= maxRange * maxRange;
    }
}
