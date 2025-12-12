package com.murilloskills.network;

import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server-to-Client payload to notify about XP gains for toast notifications.
 */
public record XpGainS2CPayload(
        String skillName,
        int xpAmount,
        String source) implements CustomPayload {

    public static final CustomPayload.Id<XpGainS2CPayload> ID = new CustomPayload.Id<>(
            Identifier.of("murilloskills", "xp_gain"));

    public static final PacketCodec<RegistryByteBuf, XpGainS2CPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, XpGainS2CPayload::skillName,
            PacketCodecs.VAR_INT, XpGainS2CPayload::xpAmount,
            PacketCodecs.STRING, XpGainS2CPayload::source,
            XpGainS2CPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    /**
     * Helper to get the skill enum from the name.
     */
    public MurilloSkillsList getSkill() {
        try {
            return MurilloSkillsList.valueOf(skillName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
