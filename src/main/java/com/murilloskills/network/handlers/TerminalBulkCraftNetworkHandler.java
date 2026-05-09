package com.murilloskills.network.handlers;

import com.murilloskills.integration.TerminalBulkCraftService;
import com.murilloskills.network.TerminalBulkCraftC2SPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles bulk crafting requests from the Tom's Storage crafting terminal UI.
 */
public final class TerminalBulkCraftNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-TerminalBulkCraft");

    private TerminalBulkCraftNetworkHandler() {
    }

    public static ServerPlayNetworking.PlayPayloadHandler<TerminalBulkCraftC2SPayload> create() {
        return (payload, context) -> context.server().execute(() -> {
            try {
                TerminalBulkCraftService.start(context.player(), payload.amount());
            } catch (Exception e) {
                LOGGER.error("Failed to start terminal bulk craft", e);
                context.player().sendMessage(Text.translatable("murilloskills.terminal_bulk_craft.failed"), false);
            }
        });
    }
}
