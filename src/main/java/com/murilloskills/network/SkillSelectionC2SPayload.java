package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Client -> Server: Player has selected their 2 main skills
 * This is a permanent choice that cannot be changed once confirmed.
 */
public record SkillSelectionC2SPayload(List<MurilloSkillsList> selectedSkills) implements CustomPayload {
    
    public static final int MAX_SELECTED_SKILLS = 2;
    
    public static final CustomPayload.Id<SkillSelectionC2SPayload> ID = 
            new CustomPayload.Id<>(Identifier.of(MurilloSkills.MOD_ID, "skill_selection"));

    public static final PacketCodec<RegistryByteBuf, SkillSelectionC2SPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeInt(payload.selectedSkills.size());
                for (MurilloSkillsList skill : payload.selectedSkills) {
                    buf.writeEnumConstant(skill);
                }
            },
            (buf) -> {
                int size = buf.readInt();
                List<MurilloSkillsList> skills = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    skills.add(buf.readEnumConstant(MurilloSkillsList.class));
                }
                return new SkillSelectionC2SPayload(skills);
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

