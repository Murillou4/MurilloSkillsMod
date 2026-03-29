package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server: Player pressed key to toggle Pathfinder speed boost (Explorer skill)
 * This allows Explorer players level 45+ to toggle the Speed II while sprinting effect.
 */
public record SpeedBoostToggleC2SPayload() implements CustomPayload {
    public static final CustomPayload.Id<SpeedBoostToggleC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "speed_boost_toggle"));
    public static final PacketCodec<RegistryByteBuf, SpeedBoostToggleC2SPayload> CODEC = PacketCodec
            .unit(new SpeedBoostToggleC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
