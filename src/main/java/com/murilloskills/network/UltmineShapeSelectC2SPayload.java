package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import com.murilloskills.skills.UltmineShape;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server: updates selected ultmine shape and parameters.
 */
public record UltmineShapeSelectC2SPayload(UltmineShape shape, int depth, int length) implements CustomPayload {
    public static final CustomPayload.Id<UltmineShapeSelectC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "ultmine_shape_select"));

    public static final PacketCodec<RegistryByteBuf, UltmineShapeSelectC2SPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeEnumConstant(payload.shape);
                buf.writeVarInt(payload.depth);
                buf.writeVarInt(payload.length);
            },
            (buf) -> new UltmineShapeSelectC2SPayload(
                    buf.readEnumConstant(UltmineShape.class),
                    buf.readVarInt(),
                    buf.readVarInt()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
