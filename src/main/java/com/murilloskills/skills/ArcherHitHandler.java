package com.murilloskills.skills;

import com.murilloskills.models.SkillReceptorResult;
import com.murilloskills.utils.ArcherXpGetter;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import com.murilloskills.utils.XpStreakManager;
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

        var data = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

        // Apply streak bonus
        int baseXp = result.getXpAmount();
        int streakXp = XpStreakManager.applyStreakBonus(player.getUuid(), MurilloSkillsList.ARCHER, baseXp);

        com.murilloskills.data.PlayerSkillData.XpAddResult xpResult = data.addXpToSkill(MurilloSkillsList.ARCHER,
                streakXp);

        // Check for milestone rewards
        com.murilloskills.utils.VanillaXpRewarder.checkAndRewardMilestone(player, "Arqueiro", xpResult);

        if (xpResult.leveledUp()) {
            var stats = data.getSkill(MurilloSkillsList.ARCHER);
            SkillNotifier.notifyLevelUp(player, MurilloSkillsList.ARCHER, stats.level);
        }

        SkillsNetworkUtils.syncSkills(player);

        // Send XP toast notification (with streak indicator)
        String targetName = target.getName().getString();
        int streak = XpStreakManager.getCurrentStreak(player.getUuid(), MurilloSkillsList.ARCHER);
        String source = streak > 1 ? targetName + " (x" + streak + ")" : targetName;
        com.murilloskills.utils.XpToastSender.send(player, MurilloSkillsList.ARCHER, streakXp, source);
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

        var data = player.getAttachedOrCreate(com.murilloskills.data.ModAttachments.PLAYER_SKILLS);

        // Apply streak bonus
        int baseXp = result.getXpAmount();
        int streakXp = XpStreakManager.applyStreakBonus(player.getUuid(), MurilloSkillsList.ARCHER, baseXp);

        com.murilloskills.data.PlayerSkillData.XpAddResult xpResult = data.addXpToSkill(MurilloSkillsList.ARCHER,
                streakXp);

        // Check for milestone rewards
        com.murilloskills.utils.VanillaXpRewarder.checkAndRewardMilestone(player, "Arqueiro", xpResult);

        if (xpResult.leveledUp()) {
            var stats = data.getSkill(MurilloSkillsList.ARCHER);
            SkillNotifier.notifyLevelUp(player, MurilloSkillsList.ARCHER, stats.level);
        }

        SkillsNetworkUtils.syncSkills(player);

        // Send XP toast notification (with streak indicator)
        String targetName = target.getName().getString();
        int streak = XpStreakManager.getCurrentStreak(player.getUuid(), MurilloSkillsList.ARCHER);
        String source = streak > 1 ? targetName + " Kill (x" + streak + ")" : targetName + " Kill";
        com.murilloskills.utils.XpToastSender.send(player, MurilloSkillsList.ARCHER, streakXp, source);

        // Track Archer achievements
        // Sharpshooter: headshot detection (approximation using vertical angle)
        if (target.getEyeY() > player.getEyeY() - 0.5 && target.getEyeY() < player.getEyeY() + 0.5) {
            com.murilloskills.utils.AchievementTracker.incrementAndCheck(
                    player, MurilloSkillsList.ARCHER,
                    com.murilloskills.utils.AchievementTracker.KEY_HEADSHOTS, 1);
        }

        // Sniper: long distance kill (>50 blocks)
        if (distance >= com.murilloskills.utils.AchievementTracker.SNIPER_DISTANCE) {
            com.murilloskills.utils.AchievementTracker.incrementAndCheck(
                    player, MurilloSkillsList.ARCHER,
                    com.murilloskills.utils.AchievementTracker.KEY_LONG_SHOTS, 1);
        }
    }
}
