package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server: Player pressed key to toggle drops-to-inventory for Vein Miner.
 */
public record VeinMinerDropsToggleC2SPayload() implements CustomPayload {
    public static final CustomPayload.Id<VeinMinerDropsToggleC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "vein_miner_drops_toggle"));
    public static final PacketCodec<RegistryByteBuf, VeinMinerDropsToggleC2SPayload> CODEC = PacketCodec
            .unit(new VeinMinerDropsToggleC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
