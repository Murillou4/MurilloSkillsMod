package com.murilloskills.utils;

import com.murilloskills.skills.MurilloSkillsList;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sistema de Streak/Combo para bônus de XP.
 * Ações consecutivas dentro de um período aumentam o multiplicador de XP.
 */
public class XpStreakManager {

    // Configurações
    // Configurações
    // Note: Constants replaced by SkillConfig getters

    // Dados por jogador
    private static final Map<UUID, Map<MurilloSkillsList, StreakData>> playerStreaks = new HashMap<>();

    /**
     * Registra uma ação e retorna o multiplicador de XP atual.
     * 
     * @param playerId UUID do jogador
     * @param skill    Skill em que a ação foi realizada
     * @return Multiplicador de XP (1.0 = sem bônus, 2.0 = +100%)
     */
    public static float recordAction(UUID playerId, MurilloSkillsList skill) {
        Map<MurilloSkillsList, StreakData> skillStreaks = playerStreaks.computeIfAbsent(
                playerId, k -> new HashMap<>());

        StreakData data = skillStreaks.computeIfAbsent(skill, k -> new StreakData());

        long now = System.currentTimeMillis();

        // Verificar se o streak continua ou reseta
        if (now - data.lastActionTime > SkillConfig.getStreakTimeoutMs()) {
            // Timeout - resetar streak
            data.currentStreak = 1;
        } else {
            // Dentro do tempo - aumentar streak
            data.currentStreak = Math.min(data.currentStreak + 1, SkillConfig.getMaxStreak());
        }

        data.lastActionTime = now;

        // Calcular multiplicador
        return getMultiplier(data.currentStreak);
    }

    /**
     * Obtém o streak atual de um jogador para uma skill.
     * 
     * @param playerId UUID do jogador
     * @param skill    Skill a verificar
     * @return Streak atual (0 se não houver)
     */
    public static int getCurrentStreak(UUID playerId, MurilloSkillsList skill) {
        Map<MurilloSkillsList, StreakData> skillStreaks = playerStreaks.get(playerId);
        if (skillStreaks == null)
            return 0;

        StreakData data = skillStreaks.get(skill);
        if (data == null)
            return 0;

        // Verificar timeout
        if (System.currentTimeMillis() - data.lastActionTime > SkillConfig.getStreakTimeoutMs()) {
            return 0;
        }

        return data.currentStreak;
    }

    /**
     * Calcula o multiplicador de XP baseado no nível de streak.
     * 
     * @param streak Nível de streak atual
     * @return Multiplicador (1.0 a 2.0)
     */
    public static float getMultiplier(int streak) {
        if (streak <= 1)
            return 1.0f;

        // Streak 2 = +10%, Streak 3 = +20%, ..., Streak 10 = +90%
        float bonus = (streak - 1) * SkillConfig.getStreakBonusPerLevel();
        return 1.0f + Math.min(bonus, 1.0f); // Max 2x (100% bonus)
    }

    /**
     * Aplica o multiplicador de streak ao XP base.
     * 
     * @param playerId UUID do jogador
     * @param skill    Skill para qual o XP está sendo ganho
     * @param baseXp   XP base a ser concedido
     * @return XP com bônus de streak aplicado
     */
    public static int applyStreakBonus(UUID playerId, MurilloSkillsList skill, int baseXp) {
        float multiplier = recordAction(playerId, skill);
        return Math.round(baseXp * multiplier);
    }

    /**
     * Limpa dados de streak para um jogador (usado quando desconecta).
     */
    public static void clearPlayerData(UUID playerId) {
        playerStreaks.remove(playerId);
    }

    /**
     * Dados internos de streak por skill.
     */
    private static class StreakData {
        int currentStreak = 0;
        long lastActionTime = 0;
    }
}
