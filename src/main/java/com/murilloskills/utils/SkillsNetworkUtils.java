package com.murilloskills.utils;

import com.murilloskills.network.SkillsSyncPayload;
import com.murilloskills.skills.MurilloSkillsList;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class SkillsNetworkUtils {

    public static void syncSkills(ServerPlayerEntity player) {
        com.murilloskills.data.PlayerSkillData data = player
                .getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
        data.normalizeParagonState();

        MurilloSkillsList activeParagon = data.getActiveParagonSkill();
        String paragonName = activeParagon != null ? activeParagon.name() : "null";

        List<MurilloSkillsList> paragonSkills = new ArrayList<>(data.getParagonSkills());

        // Get selected skills (or empty list if none selected)
        List<MurilloSkillsList> selectedSkills = data.selectedSkills != null
                ? new ArrayList<>(data.selectedSkills)
                : new ArrayList<>();

        // Envia o pacote com Skills + Paragons + Selected Skills + Max Selected
        ServerPlayNetworking.send(player, new SkillsSyncPayload(
                data.skills, paragonName, paragonSkills, selectedSkills, SkillConfig.getMaxSelectedSkills()));
    }
}
