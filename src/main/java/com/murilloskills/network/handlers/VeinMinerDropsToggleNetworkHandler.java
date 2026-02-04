package com.murilloskills.network.handlers;

import com.murilloskills.network.VeinMinerDropsToggleC2SPayload;
import com.murilloskills.skills.VeinMinerHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles toggle for vein miner drops-to-inventory mode.
 */
public final class VeinMinerDropsToggleNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-VeinMinerDropsHandler");

    private VeinMinerDropsToggleNetworkHandler() {
    }

    public static ServerPlayNetworking.PlayPayloadHandler<VeinMinerDropsToggleC2SPayload> create() {
        return (payload, context) -> {
            context.server().execute(() -> {
                try {
                    var player = context.player();

                    boolean nowEnabled = VeinMinerHandler.toggleDropsToInventory(player);

                    if (nowEnabled) {
                        player.sendMessage(Text.translatable("murilloskills.vein_miner.drops_inventory_enabled")
                                .formatted(Formatting.GREEN), true);
                    } else {
                        player.sendMessage(Text.translatable("murilloskills.vein_miner.drops_inventory_disabled")
                                .formatted(Formatting.GRAY), true);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error processing vein miner drops toggle", e);
                }
            });
        };
    }
}
