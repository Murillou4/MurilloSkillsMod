package com.murilloskills.network.handlers;

import com.murilloskills.integration.TerminalMachineTransferService;
import com.murilloskills.network.TerminalMachineTransferC2SPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles machine transfer requests from the Tom's Storage terminal client UI.
 */
public final class TerminalMachineTransferNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-TerminalMachineTransfer");

    private TerminalMachineTransferNetworkHandler() {
    }

    public static ServerPlayNetworking.PlayPayloadHandler<TerminalMachineTransferC2SPayload> create() {
        return (payload, context) -> context.server().execute(() -> {
            try {
                TerminalMachineTransferService.transfer(context.player(), payload.itemKey(), payload.amount(),
                        payload.targetPos(), payload.face());
            } catch (Exception e) {
                LOGGER.error("Failed to process terminal machine transfer", e);
                context.player().sendMessage(Text.translatable("murilloskills.terminal_transfer.failed"), false);
            }
        });
    }
}
