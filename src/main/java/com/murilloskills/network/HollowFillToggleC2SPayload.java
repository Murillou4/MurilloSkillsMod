package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server: Player pressed key to toggle hollow/filled mode for
 * Creative Brush
 */
public record HollowFillToggleC2SPayload() implements CustomPayload {
    public static final CustomPayload.Id<HollowFillToggleC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "hollow_fill_toggle"));
    public static final PacketCodec<RegistryByteBuf, HollowFillToggleC2SPayload> CODEC = PacketCodec
            .unit(new HollowFillToggleC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
