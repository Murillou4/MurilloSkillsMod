package com.murilloskills.utils;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.murilloskills.data.PlayerSkillData.XpAddResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerencia recompensas de XP vanilla (orbes de experiência do Minecraft)
 * quando jogadores atingem milestones de skill.
 */
public class VanillaXpRewarder {

    /**
     * Verifica se o jogador atravessou algum milestone e concede XP vanilla.
     */
    public static void checkAndRewardMilestone(ServerPlayerEntity player, String skillName, XpAddResult result) {
        for (int milestone : getCrossedMilestones(result)) {
            rewardMilestoneXp(player, skillName, milestone);
        }
    }

    public static List<Integer> getCrossedMilestones(XpAddResult result) {
        List<Integer> crossedMilestones = new ArrayList<>();
        if (!result.leveledUp()) {
            return crossedMilestones;
        }

        for (int milestone : SkillConfig.getSkillMilestones()) {
            if (result.oldLevel() < milestone && result.newLevel() >= milestone) {
                crossedMilestones.add(milestone);
            }
        }

        return crossedMilestones;
    }

    private static void rewardMilestoneXp(ServerPlayerEntity player, String skillName, int milestone) {
        int xpLevels = SkillConfig.getMilestoneVanillaXpLevels(milestone);
        if (xpLevels <= 0)
            return;

        player.addExperienceLevels(xpLevels);

        MutableText message = Text.empty()
                .append(Text.literal("⭐ ").formatted(Formatting.GOLD))
                .append(Text.translatable("murilloskills.notify.milestone").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal(" " + skillName + " " + milestone).formatted(Formatting.YELLOW))
                .append(Text.literal(" | ").formatted(Formatting.DARK_GRAY))
                .append(Text.translatable("murilloskills.notify.xp_levels", xpLevels).formatted(Formatting.GREEN,
                        Formatting.BOLD));

        player.sendMessage(message, false);

        // Show first-time hint about milestones
        FirstTimeHints.showHintIfFirstTime(player, FirstTimeHints.HintType.MILESTONE_REACHED);

        ServerWorld world = (ServerWorld) player.getEntityWorld();
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f,
                1.5f);
    }
}
