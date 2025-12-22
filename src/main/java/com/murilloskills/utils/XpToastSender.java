package com.murilloskills.utils;

import com.murilloskills.network.XpGainS2CPayload;
import com.murilloskills.skills.MurilloSkillsList;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Utility class to send XP gain notifications to clients for toast display.
 */
public class XpToastSender {

    /**
     * Sends an XP gain notification to the client for toast display.
     * 
     * @param player   The player who gained XP
     * @param skill    The skill that gained XP
     * @param xpAmount Amount of XP gained
     * @param source   Source of XP (e.g., "Diamante", "Zombie") - will be shown in
     *                 parentheses
     */
    public static void send(ServerPlayerEntity player, MurilloSkillsList skill, int xpAmount, String source) {
        if (player == null || skill == null || xpAmount <= 0)
            return;

        try {
            // Only send toast if player has selected this skill
            var playerData = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

            if (!playerData.isSkillSelected(skill)) {
                return; // Don't show toast for unselected skills
            }

            ServerPlayNetworking.send(player, new XpGainS2CPayload(
                    skill.name(),
                    xpAmount,
                    source != null ? source : ""));
        } catch (Exception e) {
            // Silently fail - toast is not critical
        }
    }

    /**
     * Sends an XP gain notification without source info.
     */
    public static void send(ServerPlayerEntity player, MurilloSkillsList skill, int xpAmount) {
        send(player, skill, xpAmount, null);
    }
}
