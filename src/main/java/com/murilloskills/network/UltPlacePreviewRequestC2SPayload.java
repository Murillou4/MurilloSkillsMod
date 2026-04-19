package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public record UltPlacePreviewRequestC2SPayload(BlockPos targetPos, Direction face, Vec3d hitPos,
        long requestKey) implements CustomPayload {
    public static final Id<UltPlacePreviewRequestC2SPayload> ID = new Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "ultplace_preview_request"));

    public static final PacketCodec<RegistryByteBuf, UltPlacePreviewRequestC2SPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeBlockPos(payload.targetPos);
                buf.writeEnumConstant(payload.face);
                buf.writeDouble(payload.hitPos.x);
                buf.writeDouble(payload.hitPos.y);
                buf.writeDouble(payload.hitPos.z);
                buf.writeVarLong(payload.requestKey);
            },
            (buf) -> new UltPlacePreviewRequestC2SPayload(
                    buf.readBlockPos(),
                    buf.readEnumConstant(Direction.class),
                    new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readVarLong()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
