package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UltPlaceUndoC2SPayload() implements CustomPayload {
    public static final Id<UltPlaceUndoC2SPayload> ID = new Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "ultplace_undo"));

    public static final PacketCodec<RegistryByteBuf, UltPlaceUndoC2SPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
            },
            (buf) -> new UltPlaceUndoC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
