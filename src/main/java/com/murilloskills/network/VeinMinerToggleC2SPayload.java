package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server: Player holding/releasing Vein Miner key.
 *
 * @param activated true when key is pressed, false when released
 */
public record VeinMinerToggleC2SPayload(boolean activated) implements CustomPayload {
    public static final CustomPayload.Id<VeinMinerToggleC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "vein_miner_toggle"));
    public static final PacketCodec<RegistryByteBuf, VeinMinerToggleC2SPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, VeinMinerToggleC2SPayload::activated,
            VeinMinerToggleC2SPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
