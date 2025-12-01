package com.murilloskills.network;

import com.murilloskills.MurilloSkills;
import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public record SkillsSyncPayload(Map<MurilloSkillsList, SkillGlobalState.SkillStats> skills, String paragonSkillName) implements CustomPayload {

    public static final CustomPayload.Id<SkillsSyncPayload> ID = new CustomPayload.Id<>(Identifier.of(MurilloSkills.MOD_ID, "skills_sync"));

    public static final PacketCodec<RegistryByteBuf, SkillsSyncPayload> CODEC = PacketCodec.tuple(
            PacketCodec.ofStatic(
                    (buf, val) -> {
                        buf.writeInt(val.size());
                        val.forEach((k, v) -> {
                            buf.writeEnumConstant(k);
                            buf.writeInt(v.level);
                            buf.writeDouble(v.xp);
                            buf.writeLong(v.lastAbilityUse); // ADICIONADO: Sync do tempo de uso
                        });
                    },
                    (buf) -> {
                        int size = buf.readInt();
                        Map<MurilloSkillsList, SkillGlobalState.SkillStats> map = new HashMap<>();
                        for (int i = 0; i < size; i++) {
                            MurilloSkillsList key = buf.readEnumConstant(MurilloSkillsList.class);
                            int level = buf.readInt();
                            double xp = buf.readDouble();
                            long lastUse = buf.readLong(); // ADICIONADO: Leitura do tempo
                            map.put(key, new SkillGlobalState.SkillStats(level, xp, lastUse));
                        }
                        return map;
                    }
            ),
            SkillsSyncPayload::skills,
            PacketCodecs.STRING,
            SkillsSyncPayload::paragonSkillName,
            SkillsSyncPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}