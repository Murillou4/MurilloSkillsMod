package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server: Player updates magnet configuration (enabled + range).
 */
public record MagnetConfigC2SPayload(boolean enabled, int range) implements CustomPayload {
    public static final CustomPayload.Id<MagnetConfigC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "magnet_config"));
    public static final PacketCodec<RegistryByteBuf, MagnetConfigC2SPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeBoolean(payload.enabled);
                buf.writeVarInt(payload.range);
            },
            buf -> new MagnetConfigC2SPayload(buf.readBoolean(), buf.readVarInt()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
