package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server -> Client: Sync Pathfinder speed boost active state.
 * Used to show/hide the "Pathfinder" HUD indicator on the client.
 */
public record PathfinderSyncS2CPayload(boolean active) implements CustomPayload {
    public static final CustomPayload.Id<PathfinderSyncS2CPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "pathfinder_sync"));
    public static final PacketCodec<RegistryByteBuf, PathfinderSyncS2CPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, PathfinderSyncS2CPayload::active,
            PathfinderSyncS2CPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
