package com.murilloskills.network.handlers;

import com.murilloskills.network.VeinMinerStorageDropToggleC2SPayload;
import com.murilloskills.skills.VeinMinerHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles toggle for routing ultmine drops to the bound Tom's Storage terminal.
 */
public final class VeinMinerStorageDropToggleNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-VeinMinerStorageDropHandler");

    private VeinMinerStorageDropToggleNetworkHandler() {
    }

    public static ServerPlayNetworking.PlayPayloadHandler<VeinMinerStorageDropToggleC2SPayload> create() {
        return (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();
                    VeinMinerHandler.setDropsToStorage(player, payload.enabled());

                    if (payload.enabled()) {
                        player.sendMessage(Text.translatable("murilloskills.vein_miner.drops_storage_enabled")
                                .formatted(Formatting.AQUA), true);
                    } else {
                        player.sendMessage(Text.translatable("murilloskills.vein_miner.drops_storage_disabled")
                                .formatted(Formatting.GRAY), true);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error processing vein miner storage drop toggle", e);
                }
            });
        };
    }
}
