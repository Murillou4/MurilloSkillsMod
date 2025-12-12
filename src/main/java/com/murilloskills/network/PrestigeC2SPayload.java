package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server: Player clicked the prestige button for a skill.
 * Prestige resets the skill to level 1 but grants permanent bonuses.
 */
public record PrestigeC2SPayload(MurilloSkillsList skill) implements CustomPayload {
    public static final CustomPayload.Id<PrestigeC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "prestige"));

    public static final PacketCodec<RegistryByteBuf, PrestigeC2SPayload> CODEC = PacketCodec.tuple(
            PacketCodec.ofStatic(
                    PacketByteBuf::writeEnumConstant,
                    (buf) -> buf.readEnumConstant(MurilloSkillsList.class)),
            PrestigeC2SPayload::skill,
            PrestigeC2SPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
