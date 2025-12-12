package com.murilloskills.utils;

import com.murilloskills.data.SkillGlobalState;
import com.murilloskills.skills.MurilloSkillsList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Sistema de Prestige para skills.
 * Quando uma skill atinge nível 100, o jogador pode "prestigiar" para:
 * - Resetar a skill para nível 1
 * - Ganhar um nível de prestígio (máx 10)
 * - Receber bônus permanentes por prestígio
 * 
 * Bônus por nível de prestígio:
 * - +5% XP gain para essa skill
 * - +2% efetividade dos bônus passivos
 * - Cosmético: ícone/cor diferenciado na GUI
 */
public class PrestigeManager {

    // Configurações
    public static final int MAX_PRESTIGE_LEVEL = 100;
    public static final float XP_BONUS_PER_PRESTIGE = 0.05f; // +5% XP por prestígio
    public static final float PASSIVE_BONUS_PER_PRESTIGE = 0.02f; // +2% efetividade por prestígio

    /**
     * Verifica se uma skill pode ser prestigiada.
     * Requer nível 100 e prestígio atual < 10.
     */
    public static boolean canPrestige(ServerPlayerEntity player, MurilloSkillsList skill) {
        SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
        SkillGlobalState.PlayerSkillData data = state.getPlayerData(player);
        SkillGlobalState.SkillStats stats = data.getSkill(skill);

        return stats.level >= 100 && stats.prestige < MAX_PRESTIGE_LEVEL;
    }

    /**
     * Executa o prestígio de uma skill.
     * - Incrementa nível de prestígio
     * - Reseta nível para 1
     * - Reseta XP para 0
     * - Mantém cooldowns
     * 
     * @return true se o prestígio foi executado com sucesso
     */
    public static boolean doPrestige(ServerPlayerEntity player, MurilloSkillsList skill) {
        if (!canPrestige(player, skill)) {
            return false;
        }

        SkillGlobalState state = SkillGlobalState.getServerState(player.getEntityWorld().getServer());
        SkillGlobalState.PlayerSkillData data = state.getPlayerData(player);
        SkillGlobalState.SkillStats stats = data.getSkill(skill);

        // Incrementar prestígio
        int newPrestige = stats.prestige + 1;
        stats.prestige = newPrestige;

        // Resetar nível e XP
        stats.level = 1;
        stats.xp = 0;

        state.markDirty();
        SkillsNetworkUtils.syncSkills(player);

        // Notificar jogador
        notifyPrestige(player, skill, newPrestige);

        // Efeitos visuais e sonoros
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);

        // Grant advancement for prestige
        AdvancementGranter.grantPrestigeAdvancement(player, skill, newPrestige);

        return true;
    }

    /**
     * Calcula o multiplicador de XP baseado no nível de prestígio.
     * Usado ao adicionar XP para aplicar bônus de prestígio.
     */
    public static float getXpMultiplier(int prestigeLevel) {
        return 1.0f + (prestigeLevel * XP_BONUS_PER_PRESTIGE);
    }

    /**
     * Calcula o multiplicador de efetividade baseado no nível de prestígio.
     * Usado para melhorar passivas da skill.
     */
    public static float getPassiveMultiplier(int prestigeLevel) {
        return 1.0f + (prestigeLevel * PASSIVE_BONUS_PER_PRESTIGE);
    }

    /**
     * Retorna o símbolo de prestígio para exibição na GUI.
     */
    public static String getPrestigeSymbol(int prestigeLevel) {
        if (prestigeLevel <= 0)
            return "";

        return switch (prestigeLevel) {
            case 1 -> "⚔";
            case 2 -> "⚔⚔";
            case 3 -> "★";
            case 4 -> "★★";
            case 5 -> "✦";
            case 6 -> "✦✦";
            case 7 -> "✦✦✦";
            case 8 -> "♦";
            case 9 -> "♦♦";
            case 10 -> "👑";
            default -> "P" + prestigeLevel;
        };
    }

    /**
     * Retorna a cor associada ao nível de prestígio.
     */
    public static int getPrestigeColor(int prestigeLevel) {
        return switch (prestigeLevel) {
            case 1, 2 -> 0xFF88FF88; // Verde claro
            case 3, 4 -> 0xFF88FFFF; // Ciano
            case 5, 6 -> 0xFFFFFF88; // Amarelo
            case 7, 8 -> 0xFFFF88FF; // Magenta
            case 9, 10 -> 0xFFFFDD00; // Dourado
            default -> 0xFFFFFFFF; // Branco
        };
    }

    private static void notifyPrestige(ServerPlayerEntity player, MurilloSkillsList skill, int newPrestige) {
        Text message = Text.empty()
                .append(Text.literal("🌟 ").formatted(Formatting.GOLD))
                .append(Text.translatable("murilloskills.notify.prestige").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal(" | ").formatted(Formatting.DARK_GRAY))
                .append(Text.translatable("murilloskills.skill.name." + skill.name().toLowerCase())
                        .formatted(Formatting.YELLOW))
                .append(Text.literal(" » ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(getPrestigeSymbol(newPrestige) + " P" + newPrestige)
                        .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));

        player.sendMessage(message, false);

        // Bônus info
        int xpBonus = (int) (newPrestige * XP_BONUS_PER_PRESTIGE * 100);
        int passiveBonus = (int) (newPrestige * PASSIVE_BONUS_PER_PRESTIGE * 100);

        Text bonusText = Text.translatable("murilloskills.prestige.bonus", xpBonus, passiveBonus)
                .formatted(Formatting.GRAY);
        player.sendMessage(bonusText, false);
    }
}
