package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server: toggle ultmine drops routing into the bound Tom's Storage terminal.
 */
public record VeinMinerStorageDropToggleC2SPayload(boolean enabled) implements CustomPayload {
    public static final CustomPayload.Id<VeinMinerStorageDropToggleC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "vein_miner_storage_drop_toggle"));
    public static final PacketCodec<RegistryByteBuf, VeinMinerStorageDropToggleC2SPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> buf.writeBoolean(payload.enabled),
            buf -> new VeinMinerStorageDropToggleC2SPayload(buf.readBoolean()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
