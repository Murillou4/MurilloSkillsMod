package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server -> Client: Sync area planting toggle state and show feedback
 */
public record AreaPlantingSyncS2CPayload(boolean enabled) implements CustomPayload {
    public static final CustomPayload.Id<AreaPlantingSyncS2CPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "area_planting_sync"));
    public static final PacketCodec<RegistryByteBuf, AreaPlantingSyncS2CPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, AreaPlantingSyncS2CPayload::enabled,
            AreaPlantingSyncS2CPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
