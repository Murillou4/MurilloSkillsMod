package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Client -> Server: executes a right-click action using the selected Ultmine
 * shape.
 */
public record UltmineUseC2SPayload(BlockPos targetPos, Direction face, Hand hand, Vec3d hitPos)
        implements CustomPayload {
    public static final Id<UltmineUseC2SPayload> ID = new Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "ultmine_use"));

    public static final PacketCodec<RegistryByteBuf, UltmineUseC2SPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeBlockPos(payload.targetPos);
                buf.writeEnumConstant(payload.face);
                buf.writeEnumConstant(payload.hand);
                buf.writeDouble(payload.hitPos.x);
                buf.writeDouble(payload.hitPos.y);
                buf.writeDouble(payload.hitPos.z);
            },
            (buf) -> new UltmineUseC2SPayload(
                    buf.readBlockPos(),
                    buf.readEnumConstant(Direction.class),
                    buf.readEnumConstant(Hand.class),
                    new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble())));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
