package com.murilloskills.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Server-to-client payload for Treasure Hunter effect.
 * Sends positions of chests and spawners to highlight.
 */
public record TreasureHunterS2CPayload(List<BlockPos> positions) implements CustomPayload {

    public static final CustomPayload.Id<TreasureHunterS2CPayload> ID = new CustomPayload.Id<>(
            Identifier.of("murilloskills", "treasure_hunter"));

    public static final PacketCodec<RegistryByteBuf, TreasureHunterS2CPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()),
            TreasureHunterS2CPayload::positions,
            TreasureHunterS2CPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
