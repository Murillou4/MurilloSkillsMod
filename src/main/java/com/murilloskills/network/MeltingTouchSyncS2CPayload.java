package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MeltingTouchSyncS2CPayload(boolean enabled) implements CustomPayload {
    public static final CustomPayload.Id<MeltingTouchSyncS2CPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "melting_touch_sync"));
    public static final PacketCodec<RegistryByteBuf, MeltingTouchSyncS2CPayload> CODEC = PacketCodec.tuple(
            NetworkPayloadCodecs.BOOLEAN, MeltingTouchSyncS2CPayload::enabled,
            MeltingTouchSyncS2CPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
