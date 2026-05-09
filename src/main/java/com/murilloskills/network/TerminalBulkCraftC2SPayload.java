package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server: starts a bounded bulk craft job from the current Tom's
 * Storage crafting terminal result.
 */
public record TerminalBulkCraftC2SPayload(int amount) implements CustomPayload {
    public static final Id<TerminalBulkCraftC2SPayload> ID = new Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "terminal_bulk_craft"));

    public static final PacketCodec<RegistryByteBuf, TerminalBulkCraftC2SPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> buf.writeVarInt(payload.amount),
            buf -> new TerminalBulkCraftC2SPayload(buf.readVarInt()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
