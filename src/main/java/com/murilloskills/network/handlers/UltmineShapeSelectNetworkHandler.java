package com.murilloskills.network.handlers;

import com.murilloskills.network.UltmineShapeSelectC2SPayload;
import com.murilloskills.skills.VeinMinerHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles client shape selection for Ultmine.
 */
public final class UltmineShapeSelectNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-UltmineShapeSelect");

    private UltmineShapeSelectNetworkHandler() {
    }

    public static ServerPlayNetworking.PlayPayloadHandler<UltmineShapeSelectC2SPayload> create() {
        return (payload, context) -> context.server().execute(() -> {
            try {
                var player = context.player();
                VeinMinerHandler.setUltmineSelection(player, payload.shape(), payload.depth(), payload.length());
                player.sendMessage(
                        Text.translatable("murilloskills.ultmine.selected", Text.translatable(payload.shape().getTranslationKey()))
                                .formatted(Formatting.AQUA),
                        true);
            } catch (Exception e) {
                LOGGER.error("Failed to process ultmine shape selection", e);
            }
        });
    }
}
