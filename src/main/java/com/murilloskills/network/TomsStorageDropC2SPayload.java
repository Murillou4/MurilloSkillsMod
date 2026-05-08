package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server: asks Tom's Simple Storage terminal compat to drop one
 * visible storage stack from the hovered terminal slot.
 */
public record TomsStorageDropC2SPayload(int slotId) implements CustomPayload {
    public static final CustomPayload.Id<TomsStorageDropC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "toms_storage_drop"));

    public static final PacketCodec<RegistryByteBuf, TomsStorageDropC2SPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> buf.writeVarInt(payload.slotId),
            (buf) -> new TomsStorageDropC2SPayload(buf.readVarInt()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
