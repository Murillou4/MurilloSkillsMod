package com.murilloskills.utils;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Sistema de First-Time Hints para guiar novos jogadores.
 * Mostra dicas contextuais apenas na primeira vez que o jogador realiza certas
 * ações.
 */
public class FirstTimeHints {

    // Tipos de hints disponíveis
    public enum HintType {
        SKILL_MENU_OPENED, // Primeira vez abrindo o menu de skills
        FIRST_XP_GAIN, // Primeiro ganho de XP em qualquer skill
        ABILITY_UNLOCKED, // Primeira habilidade desbloqueada (nível 100)
        PERK_UNLOCKED, // Primeiro perk desbloqueado
        STREAK_STARTED, // Primeiro combo/streak iniciado
        PARAGON_EXPLAINED, // Explicação do sistema Paragon
        SKILL_SELECTION, // Dica sobre seleção de skills
        MILESTONE_REACHED // Primeiro milestone alcançado
    }

    // Armazena quais hints cada jogador já viu
    private static final Map<UUID, Set<HintType>> seenHints = new HashMap<>();

    /**
     * Verifica se o jogador já viu uma dica e, se não, mostra e marca como vista.
     * 
     * @param player   Jogador para mostrar a dica
     * @param hintType Tipo de dica
     * @return true se a dica foi mostrada (primeira vez), false se já foi vista
     */
    public static boolean showHintIfFirstTime(ServerPlayerEntity player, HintType hintType) {
        UUID playerId = player.getUuid();

        Set<HintType> playerHints = seenHints.computeIfAbsent(playerId, k -> new HashSet<>());

        if (playerHints.contains(hintType)) {
            return false; // Já viu esta dica
        }

        // Marcar como vista e mostrar
        playerHints.add(hintType);
        showHint(player, hintType);
        return true;
    }

    /**
     * Mostra a dica correspondente ao tipo.
     */
    private static void showHint(ServerPlayerEntity player, HintType hintType) {
        Text hintText = getHintText(hintType);
        if (hintText != null) {
            player.sendMessage(hintText, false);
        }
    }

    /**
     * Retorna o texto da dica para cada tipo.
     */
    private static Text getHintText(HintType hintType) {
        return switch (hintType) {
            case SKILL_MENU_OPENED -> Text.empty()
                    .append(Text.literal("💡 ").formatted(Formatting.YELLOW))
                    .append(Text.translatable("murilloskills.hint.skill_menu").formatted(Formatting.GRAY));

            case FIRST_XP_GAIN -> Text.empty()
                    .append(Text.literal("💡 ").formatted(Formatting.YELLOW))
                    .append(Text.translatable("murilloskills.hint.first_xp").formatted(Formatting.GRAY));

            case ABILITY_UNLOCKED -> Text.empty()
                    .append(Text.literal("🎉 ").formatted(Formatting.GOLD))
                    .append(Text.translatable("murilloskills.hint.ability_unlocked").formatted(Formatting.YELLOW));

            case PERK_UNLOCKED -> Text.empty()
                    .append(Text.literal("✨ ").formatted(Formatting.AQUA))
                    .append(Text.translatable("murilloskills.hint.perk_unlocked").formatted(Formatting.GRAY));

            case STREAK_STARTED -> Text.empty()
                    .append(Text.literal("🔥 ").formatted(Formatting.GOLD))
                    .append(Text.translatable("murilloskills.hint.streak_started").formatted(Formatting.GRAY));

            case PARAGON_EXPLAINED -> Text.empty()
                    .append(Text.literal("⭐ ").formatted(Formatting.LIGHT_PURPLE))
                    .append(Text.translatable("murilloskills.hint.paragon").formatted(Formatting.GRAY));

            case SKILL_SELECTION -> Text.empty()
                    .append(Text.literal("💡 ").formatted(Formatting.YELLOW))
                    .append(Text.translatable("murilloskills.hint.skill_selection").formatted(Formatting.GRAY));

            case MILESTONE_REACHED -> Text.empty()
                    .append(Text.literal("🏆 ").formatted(Formatting.GOLD))
                    .append(Text.translatable("murilloskills.hint.milestone").formatted(Formatting.GRAY));
        };
    }

    /**
     * Verifica se o jogador já viu uma dica (sem mostrar).
     */
    public static boolean hasSeenHint(UUID playerId, HintType hintType) {
        Set<HintType> playerHints = seenHints.get(playerId);
        return playerHints != null && playerHints.contains(hintType);
    }

    /**
     * Limpa os hints vistos de um jogador (para debug/reset).
     */
    public static void resetHints(UUID playerId) {
        seenHints.remove(playerId);
    }

    /**
     * Limpa dados quando jogador desconecta (opcional - para economia de memória).
     * Note: Em produção, considere persistir em NBT para manter entre sessões.
     */
    public static void clearPlayerData(UUID playerId) {
        // Mantemos os dados para persistência entre sessões
        // Se quiser limpar: seenHints.remove(playerId);
    }
}
