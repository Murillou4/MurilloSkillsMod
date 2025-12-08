package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server: Player pressed key to toggle Night Vision (Explorer skill)
 * This allows Explorer players level 35+ to toggle Night Vision independently
 * from the main ability key (which activates Treasure Hunter at level 100).
 */
public record NightVisionToggleC2SPayload() implements CustomPayload {
    public static final CustomPayload.Id<NightVisionToggleC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "night_vision_toggle"));
    public static final PacketCodec<RegistryByteBuf, NightVisionToggleC2SPayload> CODEC = PacketCodec
            .unit(new NightVisionToggleC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
