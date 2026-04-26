package com.murilloskills.network.handlers;

import com.murilloskills.network.UltPlaceConfigC2SPayload;
import com.murilloskills.skills.UltPlaceHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UltPlaceConfigNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-UltPlaceConfig");

    private UltPlaceConfigNetworkHandler() {
    }

    public static ServerPlayNetworking.PlayPayloadHandler<UltPlaceConfigC2SPayload> create() {
        return (payload, context) -> context.server().execute(() -> {
            try {
                var player = context.player();
                boolean previousEnabled = UltPlaceHandler.isEnabled(player);

                if (payload.enabled() && !UltPlaceHandler.canUseUltPlace(player)) {
                    player.sendMessage(Text.translatable("murilloskills.builder.need_builder_skill")
                            .formatted(Formatting.RED), true);
                    UltPlaceHandler.setSelection(player, payload.shape(), payload.size(), payload.length(),
                            payload.height(), payload.variant(), payload.anchorMode(), payload.rotationMode(),
                            payload.spacing(), false);
                    return;
                }

                UltPlaceHandler.setSelection(player, payload.shape(), payload.size(), payload.length(),
                        payload.height(), payload.variant(), payload.anchorMode(), payload.rotationMode(), payload.spacing(),
                        payload.enabled());

                if (previousEnabled != payload.enabled()) {
                    String messageKey = payload.enabled()
                            ? "murilloskills.ultplace.enabled.on"
                            : "murilloskills.ultplace.enabled.off";
                    player.sendMessage(Text.translatable(messageKey)
                            .formatted(payload.enabled() ? Formatting.AQUA : Formatting.GRAY), true);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to update UltPlace config", e);
            }
        });
    }
}
