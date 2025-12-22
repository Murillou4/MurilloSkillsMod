package com.murilloskills.utils;

import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SkillNotifier {

        public static void notifyLevelUp(ServerPlayerEntity player, MurilloSkillsList skill, int newLevel) {
                notifyLevelUp(player, skill, newLevel - 1, newLevel);
        }

        public static void notifyLevelUp(ServerPlayerEntity player, MurilloSkillsList skill, int oldLevel,
                        int newLevel) {
                // Mensagem Minimalista Otimizada
                Text message = Text.empty()
                                .append(Text.literal("✦ ").formatted(Formatting.GOLD))
                                .append(Text.translatable("murilloskills.notify.level_up").formatted(Formatting.GOLD,
                                                Formatting.BOLD))
                                .append(Text.literal(" | ").formatted(Formatting.DARK_GRAY))
                                .append(Text.translatable("murilloskills.skill.name." + skill.name().toLowerCase())
                                                .formatted(Formatting.YELLOW))
                                .append(Text.literal(" » ").formatted(Formatting.DARK_GRAY))
                                .append(Text.literal(String.valueOf(newLevel)).formatted(Formatting.WHITE,
                                                Formatting.BOLD));

                player.sendMessage(message, false); // Chat

                // Som (Executado no local do player para ele ouvir)
                player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS,
                                SkillConfig.getLevelUpVolume(), SkillConfig.getLevelUpPitch());

                // Grant advancement for milestone
                AdvancementGranter.checkAndGrantAdvancement(player, skill, oldLevel, newLevel);

                // Track daily level-ups for Speed Leveler achievement
                AchievementTracker.incrementAndCheck(player, skill, AchievementTracker.KEY_LEVELS_TODAY, 1);
        }

}