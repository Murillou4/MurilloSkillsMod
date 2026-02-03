package com.murilloskills.network.handlers;

import com.murilloskills.network.VeinMinerToggleC2SPayload;
import com.murilloskills.skills.VeinMinerHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles toggle for Vein Miner feature (global, not tied to skills).
 */
public final class VeinMinerToggleNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-VeinMinerHandler");

    private VeinMinerToggleNetworkHandler() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a handler for vein miner toggle requests.
     *
     * @return ServerPlayNetworking handler for VeinMinerToggleC2SPayload
     */
    public static ServerPlayNetworking.PlayPayloadHandler<VeinMinerToggleC2SPayload> create() {
        return (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    boolean nowEnabled = VeinMinerHandler.toggleVeinMiner(player);

                    if (nowEnabled) {
                        player.sendMessage(Text.translatable("murilloskills.vein_miner.enabled")
                                .formatted(Formatting.GREEN), true);
                    } else {
                        player.sendMessage(Text.translatable("murilloskills.vein_miner.disabled")
                                .formatted(Formatting.GRAY), true);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error processing vein miner toggle", e);
                }
            });
        };
    }
}
