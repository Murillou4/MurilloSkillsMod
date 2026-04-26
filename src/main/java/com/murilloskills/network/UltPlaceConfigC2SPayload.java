package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import com.murilloskills.skills.UltPlaceAnchorMode;
import com.murilloskills.skills.UltPlaceRotationMode;
import com.murilloskills.skills.UltPlaceShape;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UltPlaceConfigC2SPayload(UltPlaceShape shape, int size, int length, int height, int variant,
        UltPlaceAnchorMode anchorMode, UltPlaceRotationMode rotationMode,
        int spacing, boolean enabled) implements CustomPayload {
    public static final Id<UltPlaceConfigC2SPayload> ID = new Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "ultplace_config"));

    public static final PacketCodec<RegistryByteBuf, UltPlaceConfigC2SPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeEnumConstant(payload.shape);
                buf.writeVarInt(payload.size);
                buf.writeVarInt(payload.length);
                buf.writeVarInt(payload.height);
                buf.writeVarInt(payload.variant);
                buf.writeEnumConstant(payload.anchorMode);
                buf.writeEnumConstant(payload.rotationMode);
                buf.writeVarInt(payload.spacing);
                buf.writeBoolean(payload.enabled);
            },
            (buf) -> new UltPlaceConfigC2SPayload(
                    buf.readEnumConstant(UltPlaceShape.class),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readEnumConstant(UltPlaceAnchorMode.class),
                    buf.readEnumConstant(UltPlaceRotationMode.class),
                    buf.readVarInt(),
                    buf.readBoolean()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
