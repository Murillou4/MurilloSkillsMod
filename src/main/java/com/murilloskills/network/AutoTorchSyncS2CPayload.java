package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server -> Client: Sync Auto-Torch toggle state for HUD indicator.
 */
public record AutoTorchSyncS2CPayload(boolean enabled) implements CustomPayload {
    public static final CustomPayload.Id<AutoTorchSyncS2CPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "auto_torch_sync"));
    public static final PacketCodec<RegistryByteBuf, AutoTorchSyncS2CPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, AutoTorchSyncS2CPayload::enabled,
            AutoTorchSyncS2CPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
