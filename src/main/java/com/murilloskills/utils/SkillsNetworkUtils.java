package com.murilloskills.utils;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.network.SkillsSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public class SkillsNetworkUtils {

    public static void syncSkills(ServerPlayerEntity player) {
        SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
        SkillGlobalState.PlayerSkillData data = state.getPlayerData(player);

        String paragonName = (data.paragonSkill != null) ? data.paragonSkill.name() : "null";

        // Envia o pacote com Skills + Paragon Name
        ServerPlayNetworking.send(player, new SkillsSyncPayload(data.skills, paragonName));
    }
}