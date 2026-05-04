package com.murilloskills.network.handlers;

import com.murilloskills.network.MeltingTouchToggleC2SPayload;
import com.murilloskills.skills.MeltingTouchHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MeltingTouchToggleNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-MeltingTouchHandler");

    private MeltingTouchToggleNetworkHandler() {
    }

    public static ServerPlayNetworking.PlayPayloadHandler<MeltingTouchToggleC2SPayload> create() {
        return (payload, context) -> context.server().execute(() -> {
            try {
                var player = context.player();

                if (!MeltingTouchHandler.isAvailable(player)) {
                    player.sendMessage(Text.translatable("murilloskills.melting_touch.requirement")
                            .formatted(Formatting.RED), true);
                    MeltingTouchHandler.sync(player);
                    return;
                }

                boolean enabled = MeltingTouchHandler.toggle(player);
                player.sendMessage(Text.translatable(enabled
                        ? "murilloskills.melting_touch.enabled"
                        : "murilloskills.melting_touch.disabled")
                        .formatted(enabled ? Formatting.GOLD : Formatting.GRAY), true);
            } catch (Exception e) {
                LOGGER.error("Error processing melting touch toggle", e);
            }
        });
    }
}
