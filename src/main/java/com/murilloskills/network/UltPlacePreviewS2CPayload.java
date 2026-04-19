package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record UltPlacePreviewS2CPayload(long requestKey, List<BlockPos> positions) implements CustomPayload {
    public static final Id<UltPlacePreviewS2CPayload> ID = new Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "ultplace_preview"));

    public static final PacketCodec<RegistryByteBuf, UltPlacePreviewS2CPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeVarLong(payload.requestKey);
                buf.writeVarInt(payload.positions.size());
                for (BlockPos pos : payload.positions) {
                    buf.writeBlockPos(pos);
                }
            },
            (buf) -> {
                long requestKey = buf.readVarLong();
                int size = buf.readVarInt();
                List<BlockPos> positions = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    positions.add(buf.readBlockPos());
                }
                return new UltPlacePreviewS2CPayload(requestKey, positions);
            });

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
