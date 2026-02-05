package com.murilloskills.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
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
    private static final int MAX_POSITIONS = 1024;

    public static final PacketCodec<RegistryByteBuf, TreasureHunterS2CPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeInt(payload.positions.size());
                for (BlockPos pos : payload.positions) {
                    buf.writeBlockPos(pos);
                }
            },
            (buf) -> {
                int size = buf.readInt();
                List<BlockPos> positions = new java.util.ArrayList<>();
                for (int i = 0; i < size; i++) {
                    BlockPos pos = buf.readBlockPos();
                    if (i < MAX_POSITIONS) {
                        positions.add(pos);
                    }
                }
                return new TreasureHunterS2CPayload(positions);
            });

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
