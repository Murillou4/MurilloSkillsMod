package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server: Player pressed G to toggle 3x3 area planting (Farmer skill)
 */
public record AreaPlantingToggleC2SPayload() implements CustomPayload {
    public static final CustomPayload.Id<AreaPlantingToggleC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "area_planting_toggle"));
    public static final PacketCodec<RegistryByteBuf, AreaPlantingToggleC2SPayload> CODEC = PacketCodec
            .unit(new AreaPlantingToggleC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
