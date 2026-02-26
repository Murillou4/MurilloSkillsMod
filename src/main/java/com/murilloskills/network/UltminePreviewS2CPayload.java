package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Server -> Client: validated block positions to render as Ultmine preview.
 */
public record UltminePreviewS2CPayload(List<BlockPos> positions) implements CustomPayload {
    public static final CustomPayload.Id<UltminePreviewS2CPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "ultmine_preview"));

    public static final PacketCodec<RegistryByteBuf, UltminePreviewS2CPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeVarInt(payload.positions.size());
                for (BlockPos pos : payload.positions) {
                    buf.writeBlockPos(pos);
                }
            },
            (buf) -> {
                int size = buf.readVarInt();
                List<BlockPos> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(buf.readBlockPos());
                }
                return new UltminePreviewS2CPayload(list);
            });

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
