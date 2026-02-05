package com.murilloskills.utils;

import com.murilloskills.network.SkillsSyncPayload;
import com.murilloskills.network.XpDataSyncS2CPayload;
import com.murilloskills.skills.MurilloSkillsList;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class SkillsNetworkUtils {

    public static void syncSkills(ServerPlayerEntity player) {
        com.murilloskills.data.PlayerSkillData data = player
                .getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

        String paragonName = (data.paragonSkill != null) ? data.paragonSkill.name() : "null";

        // Get selected skills (or empty list if none selected)
        List<MurilloSkillsList> selectedSkills = data.selectedSkills != null
                ? new ArrayList<>(data.selectedSkills)
                : new ArrayList<>();

        // Envia o pacote com Skills + Paragon Name + Selected Skills
        ServerPlayNetworking.send(player, new SkillsSyncPayload(data.skills, paragonName, selectedSkills));
        ServerPlayNetworking.send(player,
                XpDataSyncS2CPayload.fromData(com.murilloskills.data.XpDataManager.getCurve(),
                        com.murilloskills.data.XpDataManager.getValues()));
    }
}
