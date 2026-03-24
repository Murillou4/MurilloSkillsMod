package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server: Player sets XP direct-to-player mode.
 */
public record XpDirectToggleC2SPayload(boolean enabled) implements CustomPayload {
    public static final CustomPayload.Id<XpDirectToggleC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "xp_direct_toggle"));
    public static final PacketCodec<RegistryByteBuf, XpDirectToggleC2SPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> buf.writeBoolean(payload.enabled),
            buf -> new XpDirectToggleC2SPayload(buf.readBoolean()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
