package com.murilloskills.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-server payload for undoing the last Creative Brush action.
 */
public record BuilderUndoC2SPayload() implements CustomPayload {
    public static final CustomPayload.Id<BuilderUndoC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of("murilloskills", "builder_undo"));

    public static final PacketCodec<RegistryByteBuf, BuilderUndoC2SPayload> CODEC = PacketCodec.unit(
            new BuilderUndoC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
