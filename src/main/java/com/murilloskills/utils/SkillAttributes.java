package com.murilloskills.utils;

import com.murilloskills.api.AbstractSkill;
import com.murilloskills.api.SkillRegistry;

import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkillAttributes {

    private static final Logger LOGGER = LoggerFactory.getLogger("MurilloSkills-Attributes");

    /**
     * Updates all stats for a player using the new skill system
     */
    /**
     * Updates all stats for a player using the new skill system
     */
    public static void updateAllStats(ServerPlayerEntity player, com.murilloskills.data.PlayerSkillData data) {
        try {
            // Update attributes for all selected skills using the registry
            if (data.hasSelectedSkills()) {
                for (MurilloSkillsList skillEnum : data.getSelectedSkills()) {
                    AbstractSkill skillObj = SkillRegistry.get(skillEnum);
                    if (skillObj != null) {
                        int level = data.getSkill(skillEnum).level;
                        skillObj.updateAttributes(player, level);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar atributos para " + player.getName().getString(), e);
        }
    }

    /**
     * Updates all stats for a player using the new skill system (fetches data
     * internally)
     */
    public static void updateAllStats(ServerPlayerEntity player) {
        com.murilloskills.data.PlayerSkillData data = player
                .getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);
        updateAllStats(player, data);
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
                int level = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS)
                        .getSkill(MurilloSkillsList.MINER).level;
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
                int level = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS)
                        .getSkill(MurilloSkillsList.WARRIOR).level;
                warriorSkill.updateAttributes(player, level);
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar stats do guerreiro (método legacy)", e);
        }
    }
}