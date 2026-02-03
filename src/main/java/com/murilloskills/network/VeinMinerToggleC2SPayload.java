package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server: Player pressed key to toggle Vein Miner.
 */
public record VeinMinerToggleC2SPayload() implements CustomPayload {
    public static final CustomPayload.Id<VeinMinerToggleC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "vein_miner_toggle"));
    public static final PacketCodec<RegistryByteBuf, VeinMinerToggleC2SPayload> CODEC = PacketCodec
            .unit(new VeinMinerToggleC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
