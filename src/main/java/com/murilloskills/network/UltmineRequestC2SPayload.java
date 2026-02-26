package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Client -> Server: asks for a validated preview for the currently selected
 * ultmine shape.
 */
public record UltmineRequestC2SPayload(BlockPos targetPos, Direction face) implements CustomPayload {
    public static final CustomPayload.Id<UltmineRequestC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "ultmine_request"));

    public static final PacketCodec<RegistryByteBuf, UltmineRequestC2SPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeBlockPos(payload.targetPos);
                buf.writeEnumConstant(payload.face);
            },
            (buf) -> new UltmineRequestC2SPayload(
                    buf.readBlockPos(),
                    buf.readEnumConstant(Direction.class)));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
