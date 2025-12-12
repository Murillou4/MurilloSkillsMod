package com.murilloskills.skills;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.models.SkillReceptorResult;
import com.murilloskills.utils.SkillAttributes;
import com.murilloskills.utils.SkillNotifier;
import com.murilloskills.utils.SkillsNetworkUtils;
import com.murilloskills.utils.WarriorXpGetter;
import com.murilloskills.utils.XpStreakManager;
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

        // Apply streak bonus
        int baseXp = result.getXpAmount();
        int streakXp = XpStreakManager.applyStreakBonus(serverPlayer.getUuid(), MurilloSkillsList.WARRIOR, baseXp);

        // --- UPDATED CALL: Handles Paragon Logic Internally ---
        SkillGlobalState.XpAddResult xpResult = data.addXpToSkill(MurilloSkillsList.WARRIOR, streakXp);

        // Check for milestone rewards
        com.murilloskills.utils.VanillaXpRewarder.checkAndRewardMilestone(serverPlayer, "Guerreiro", xpResult);

        if (xpResult.leveledUp()) {
            var stats = data.getSkill(MurilloSkillsList.WARRIOR);
            SkillNotifier.notifyLevelUp(serverPlayer, MurilloSkillsList.WARRIOR, stats.level);
            SkillAttributes.updateAllStats(serverPlayer);
        }

        state.markDirty();
        SkillsNetworkUtils.syncSkills(serverPlayer);

        // Send XP toast notification (with streak indicator)
        String mobName = victim.getName().getString();
        int streak = XpStreakManager.getCurrentStreak(serverPlayer.getUuid(), MurilloSkillsList.WARRIOR);
        String source = streak > 1 ? mobName + " (x" + streak + ")" : mobName;
        com.murilloskills.utils.XpToastSender.send(serverPlayer, MurilloSkillsList.WARRIOR, streakXp, source);

        // Track daily challenge progress - Warrior challenges
        com.murilloskills.utils.DailyChallengeManager.recordProgress(serverPlayer,
                com.murilloskills.utils.DailyChallengeManager.ChallengeType.KILL_MOBS, 1);

        // Track specific mob types
        if (victim instanceof net.minecraft.entity.mob.ZombieEntity ||
                victim instanceof net.minecraft.entity.mob.SkeletonEntity ||
                victim instanceof net.minecraft.entity.mob.PhantomEntity ||
                victim instanceof net.minecraft.entity.mob.WitherSkeletonEntity) {
            com.murilloskills.utils.DailyChallengeManager.recordProgress(serverPlayer,
                    com.murilloskills.utils.DailyChallengeManager.ChallengeType.KILL_UNDEAD, 1);
        }
        if (victim instanceof net.minecraft.entity.mob.SpiderEntity ||
                victim instanceof net.minecraft.entity.mob.CaveSpiderEntity) {
            com.murilloskills.utils.DailyChallengeManager.recordProgress(serverPlayer,
                    com.murilloskills.utils.DailyChallengeManager.ChallengeType.KILL_SPIDERS, 1);
        }

        com.murilloskills.utils.DailyChallengeManager.syncChallenges(serverPlayer);
    }
}