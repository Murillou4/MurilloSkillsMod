package com.murilloskills.skills;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.models.SkillReceptorResult;
import com.murilloskills.utils.ArcherXpGetter;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Handler para processar hits de flecha e dar XP para Archer
 */
public class ArcherHitHandler {

    /**
     * Processa um hit de flecha (quando a flecha atinge um alvo)
     * 
     * @param player   O jogador que atirou
     * @param target   O alvo atingido
     * @param distance Distância do tiro
     */
    public static void handleArrowHit(ServerPlayerEntity player, Entity target, double distance) {
        if (player == null || target == null)
            return;
        if (player.getEntityWorld().isClient())
            return;

        SkillReceptorResult result = ArcherXpGetter.getArrowHitXp(target, distance);
        if (!result.didGainXp())
            return;

        SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
        var data = state.getPlayerData(player);

        if (data.addXpToSkill(MurilloSkillsList.ARCHER, result.getXpAmount())) {
            var stats = data.getSkill(MurilloSkillsList.ARCHER);
            SkillNotifier.notifyLevelUp(player, MurilloSkillsList.ARCHER, stats.level);
        }

        state.markDirty();
        SkillsNetworkUtils.syncSkills(player);
    }

    /**
     * Processa um kill com flecha (quando a flecha mata um alvo)
     * 
     * @param player   O jogador que atirou
     * @param target   O alvo morto
     * @param distance Distância do tiro
     */
    public static void handleArrowKill(ServerPlayerEntity player, LivingEntity target, double distance) {
        if (player == null || target == null)
            return;
        if (player.getEntityWorld().isClient())
            return;

        SkillReceptorResult result = ArcherXpGetter.getArrowKillXp(target, distance);
        if (!result.didGainXp())
            return;

        SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
        var data = state.getPlayerData(player);

        if (data.addXpToSkill(MurilloSkillsList.ARCHER, result.getXpAmount())) {
            var stats = data.getSkill(MurilloSkillsList.ARCHER);
            SkillNotifier.notifyLevelUp(player, MurilloSkillsList.ARCHER, stats.level);
        }

        state.markDirty();
        SkillsNetworkUtils.syncSkills(player);
    }
}
