package com.murilloskills.skills;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.models.SkillReceptorResult;
import com.murilloskills.utils.SkillAttributes;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import com.murilloskills.utils.WarriorXpGetter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public class MobKillHandler {
    public static void handle(PlayerEntity player, Entity entityKilled) {
        if (!(entityKilled instanceof LivingEntity victim) || player.getEntityWorld().isClient())
            return;

        SkillReceptorResult result = WarriorXpGetter.getMobXp(victim);
        if (!result.didGainXp())
            return;

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        SkillGlobalState state = SkillGlobalState.getServerState(serverPlayer.getEntityWorld().getServer());
        var data = state.getPlayerData(serverPlayer);

        // --- UPDATED CALL: Handles Paragon Logic Internally ---
        if (data.addXpToSkill(MurilloSkillsList.WARRIOR, result.getXpAmount())) {
            var stats = data.getSkill(MurilloSkillsList.WARRIOR);
            SkillNotifier.notifyLevelUp(serverPlayer, MurilloSkillsList.WARRIOR, stats.level);
            SkillAttributes.updateAllStats(serverPlayer);
        }

        state.markDirty();
        SkillsNetworkUtils.syncSkills(serverPlayer);
    }
}