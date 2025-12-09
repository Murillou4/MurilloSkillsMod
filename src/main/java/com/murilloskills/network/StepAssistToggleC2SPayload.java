package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server: Player pressed key to toggle Step Assist (Explorer skill)
 * This allows Explorer players level 10+ to toggle auto step-up independently.
 */
public record StepAssistToggleC2SPayload() implements CustomPayload {
    public static final CustomPayload.Id<StepAssistToggleC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "step_assist_toggle"));
    public static final PacketCodec<RegistryByteBuf, StepAssistToggleC2SPayload> CODEC = PacketCodec
            .unit(new StepAssistToggleC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
