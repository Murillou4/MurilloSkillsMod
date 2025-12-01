package com.murilloskills.utils;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SkillNotifier {

    public static void notifyLevelUp(ServerPlayerEntity player, MurilloSkillsList skill, int newLevel) {
        // Mensagem Minimalista Otimizada
        Text message = Text.empty()
                .append(Text.literal("✦ ").formatted(Formatting.GOLD))
                .append(Text.literal("LEVEL UP!").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal(" | ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(formatSkillName(skill)).formatted(Formatting.YELLOW))
                .append(Text.literal(" » ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(String.valueOf(newLevel)).formatted(Formatting.WHITE, Formatting.BOLD));

        player.sendMessage(message, false); // Chat

        // Som (Executado no local do player para ele ouvir)
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    private static String formatSkillName(MurilloSkillsList skill) {
        // Simples capitalize ou switch case para nomes em PT-BR
        return switch (skill) {
            case MINER -> "Miner";
            case WARRIOR -> "Warrior";
            case FARMER -> "Farmer";
            case ARCHER -> "Archer";
            case FISHER -> "Fisher";
            case BUILDER -> "Builder";
            case BLACKSMITH -> "Blacksmith";
            default -> skill.name();
        };
    }
}