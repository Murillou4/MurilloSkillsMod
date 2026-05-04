package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MeltingTouchToggleC2SPayload() implements CustomPayload {
    public static final CustomPayload.Id<MeltingTouchToggleC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "melting_touch_toggle"));
    public static final PacketCodec<RegistryByteBuf, MeltingTouchToggleC2SPayload> CODEC = PacketCodec
            .unit(new MeltingTouchToggleC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
