package com.murilloskills.utils;

import com.murilloskills.api.AbstractSkill;
import com.murilloskills.api.SkillRegistry;
import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkillAttributes {

    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-Attributes");

    /**
     * Updates all stats for a player using the new skill system
     */
    public static void updateAllStats(ServerPlayerEntity player) {
        try {
            SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
            var playerData = state.getPlayerData(player);

            // Update attributes for all selected skills using the registry
            if (playerData.hasSelectedSkills()) {
                for (MurilloSkillsList skillEnum : playerData.getSelectedSkills()) {
                    AbstractSkill skillObj = SkillRegistry.get(skillEnum);
                    if (skillObj != null) {
                        int level = playerData.getSkill(skillEnum).level;
                        skillObj.updateAttributes(player, level);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar atributos para " + player.getName().getString(), e);
        }
    }

    /**
     * Legacy method for updating miner stats - now uses the new skill system
     * 
     * @deprecated Use updateAllStats() instead
     */
    @Deprecated
    public static void updateMinerStats(ServerPlayerEntity player) {
        try {
            AbstractSkill minerSkill = SkillRegistry.get(MurilloSkillsList.MINER);
            if (minerSkill != null) {
                SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
                int level = state.getPlayerData(player).getSkill(MurilloSkillsList.MINER).level;
                minerSkill.updateAttributes(player, level);
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar stats do minerador (método legacy)", e);
        }
    }

    /**
     * Legacy method for updating warrior stats - now uses the new skill system
     * 
     * @deprecated Use updateAllStats() instead
     */
    @Deprecated
    public static void updateWarriorStats(ServerPlayerEntity player) {
        try {
            AbstractSkill warriorSkill = SkillRegistry.get(MurilloSkillsList.WARRIOR);
            if (warriorSkill != null) {
                SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
                int level = state.getPlayerData(player).getSkill(MurilloSkillsList.WARRIOR).level;
                warriorSkill.updateAttributes(player, level);
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar stats do guerreiro (método legacy)", e);
        }
    }
}