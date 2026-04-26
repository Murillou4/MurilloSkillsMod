package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import com.murilloskills.data.PlayerSkillData;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SkillsSyncPayload(
        Map<MurilloSkillsList, PlayerSkillData.SkillStats> skills,
        String paragonSkillName,
        List<MurilloSkillsList> paragonSkills,
        List<MurilloSkillsList> selectedSkills,
        int maxSelectedSkills) implements CustomPayload {

    public static final CustomPayload.Id<SkillsSyncPayload> ID = new CustomPayload.Id<>(
            Identifier.of(MurilloSkills.MOD_ID, "skills_sync"));

    public static final PacketCodec<RegistryByteBuf, SkillsSyncPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                // Write skills map
                buf.writeInt(payload.skills.size());
                payload.skills.forEach((k, v) -> {
                    buf.writeEnumConstant(k);
                    buf.writeInt(v.level);
                    buf.writeDouble(v.xp);
                    buf.writeLong(v.lastAbilityUse);
                    buf.writeInt(v.prestige);
                });

                // Write paragon skill name
                buf.writeString(payload.paragonSkillName);

                // Write all paragon skills
                buf.writeInt(payload.paragonSkills.size());
                for (MurilloSkillsList skill : payload.paragonSkills) {
                    buf.writeEnumConstant(skill);
                }

                // Write selected skills list
                buf.writeInt(payload.selectedSkills.size());
                for (MurilloSkillsList skill : payload.selectedSkills) {
                    buf.writeEnumConstant(skill);
                }

                // Write max selected skills
                buf.writeVarInt(payload.maxSelectedSkills);
            },
            (buf) -> {
                // Read skills map
                int skillsSize = buf.readInt();
                Map<MurilloSkillsList, PlayerSkillData.SkillStats> skills = new HashMap<>();
                for (int i = 0; i < skillsSize; i++) {
                    MurilloSkillsList key = buf.readEnumConstant(MurilloSkillsList.class);
                    int level = buf.readInt();
                    double xp = buf.readDouble();
                    long lastUse = buf.readLong();
                    int prestige = buf.readInt();
                    skills.put(key, new PlayerSkillData.SkillStats(level, xp, lastUse, prestige));
                }

                // Read paragon skill name
                String paragonName = buf.readString();

                // Read all paragon skills
                int paragonSize = buf.readInt();
                List<MurilloSkillsList> paragonSkills = new ArrayList<>();
                for (int i = 0; i < paragonSize; i++) {
                    paragonSkills.add(buf.readEnumConstant(MurilloSkillsList.class));
                }

                // Read selected skills list
                int selectedSize = buf.readInt();
                List<MurilloSkillsList> selectedSkills = new ArrayList<>();
                for (int i = 0; i < selectedSize; i++) {
                    selectedSkills.add(buf.readEnumConstant(MurilloSkillsList.class));
                }

                // Read max selected skills
                int maxSelectedSkills = buf.readVarInt();

                return new SkillsSyncPayload(skills, paragonName, paragonSkills, selectedSkills, maxSelectedSkills);
            });

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
