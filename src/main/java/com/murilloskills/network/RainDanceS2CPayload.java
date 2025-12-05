package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server -> Client: Notify player that Rain Dance is starting/stopping
 * Used to render visual rain effect on the client side.
 */
public record RainDanceS2CPayload(boolean active, int durationTicks) implements CustomPayload {

    public static final CustomPayload.Id<RainDanceS2CPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "rain_dance"));

    public static final PacketCodec<RegistryByteBuf, RainDanceS2CPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeBoolean(payload.active);
                buf.writeInt(payload.durationTicks);
            },
            (buf) -> {
                boolean active = buf.readBoolean();
                int durationTicks = buf.readInt();
                return new RainDanceS2CPayload(active, durationTicks);
            });

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
