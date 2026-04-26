package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Client -> Server: jogador escolheu uma habilidade Paragon para ativar.
// Skill null keeps backward-compatible behavior: the server uses the active paragon.
public record SkillAbilityC2SPayload(MurilloSkillsList skill) implements CustomPayload {
    public static final CustomPayload.Id<SkillAbilityC2SPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "skill_ability_activate"));

    public SkillAbilityC2SPayload() {
        this(null);
    }

    public static final PacketCodec<RegistryByteBuf, SkillAbilityC2SPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeBoolean(payload.skill != null);
                if (payload.skill != null) {
                    buf.writeEnumConstant(payload.skill);
                }
            },
            buf -> new SkillAbilityC2SPayload(
                    buf.readBoolean() ? buf.readEnumConstant(MurilloSkillsList.class) : null));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
