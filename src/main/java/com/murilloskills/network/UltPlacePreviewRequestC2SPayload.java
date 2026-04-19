package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public record UltPlacePreviewRequestC2SPayload(BlockPos targetPos, Direction face) implements CustomPayload {
    public static final Id<UltPlacePreviewRequestC2SPayload> ID = new Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "ultplace_preview_request"));

    public static final PacketCodec<RegistryByteBuf, UltPlacePreviewRequestC2SPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeBlockPos(payload.targetPos);
                buf.writeEnumConstant(payload.face);
            },
            (buf) -> new UltPlacePreviewRequestC2SPayload(
                    buf.readBlockPos(),
                    buf.readEnumConstant(Direction.class)));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
