package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server: Player wants to reset a specific skill to level 0.
 * This resets the level and XP but keeps the skill as one of the selected
 * skills.
 */
public record SkillResetC2SPayload(MurilloSkillsList skill) implements CustomPayload {

    public static final CustomPayload.Id<SkillResetC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "skill_reset"));

    public static final PacketCodec<RegistryByteBuf, SkillResetC2SPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeEnumConstant(payload.skill);
            },
            (buf) -> {
                return new SkillResetC2SPayload(buf.readEnumConstant(MurilloSkillsList.class));
            });

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
