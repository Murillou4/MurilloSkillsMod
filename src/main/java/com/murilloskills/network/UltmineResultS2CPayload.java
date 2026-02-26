package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server -> Client: outcome of an Ultmine execution attempt.
 */
public record UltmineResultS2CPayload(boolean success, int minedBlocks, int requestedBlocks, String messageKey)
        implements CustomPayload {
    public static final CustomPayload.Id<UltmineResultS2CPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "ultmine_result"));

    public static final PacketCodec<RegistryByteBuf, UltmineResultS2CPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, UltmineResultS2CPayload::success,
            PacketCodecs.VAR_INT, UltmineResultS2CPayload::minedBlocks,
            PacketCodecs.VAR_INT, UltmineResultS2CPayload::requestedBlocks,
            PacketCodecs.STRING, UltmineResultS2CPayload::messageKey,
            UltmineResultS2CPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
