package com.murilloskills.utils;

import com.murilloskills.models.SkillReceptorResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Utility class for calculating Archer XP rewards.
 * Provides XP for arrow hits and kills based on target type and distance.
 */
public class ArcherXpGetter {

    // Base XP values for arrow hits
    private static final int XP_HIT_BASE = 5;
    private static final int XP_HIT_HOSTILE = 10;

    // Base XP values for arrow kills
    private static final int XP_KILL_BASE = 15;
    private static final int XP_KILL_HOSTILE = 25;

    // Distance thresholds for long range bonus
    private static final int LONG_RANGE_TIER_1 = 20;
    private static final int LONG_RANGE_TIER_2 = 40;
    private static final int LONG_RANGE_TIER_3 = 60;

    // Distance bonus multipliers
    private static final double LONG_RANGE_MULTIPLIER_1 = 1.5;
    private static final double LONG_RANGE_MULTIPLIER_2 = 2.0;
    private static final double LONG_RANGE_MULTIPLIER_3 = 3.0;

    /**
     * Calcula XP por acertar um alvo com flecha
     * 
     * @param target   O alvo atingido
     * @param distance Distância do tiro
     * @return SkillReceptorResult com o XP ganho
     */
    public static SkillReceptorResult getArrowHitXp(Entity target, double distance) {
        // Não dá XP para hits em jogadores (PvP pode ser abusivo)
        if (target instanceof PlayerEntity) {
            return new SkillReceptorResult(false, 0);
        }

        // Só dá XP para entidades vivas
        if (!(target instanceof LivingEntity)) {
            return new SkillReceptorResult(false, 0);
        }

        int baseXp = XP_HIT_BASE;

        // Bônus de XP para mobs hostis
        if (target instanceof HostileEntity) {
            baseXp = XP_HIT_HOSTILE;
        }

        // Bônus de XP para tiros longos (Long Range Hits)
        // Distância > 20 blocos = +50% XP
        // Distância > 40 blocos = +100% XP
        // Distância > 60 blocos = +200% XP
        if (distance > LONG_RANGE_TIER_3) {
            baseXp = (int) (baseXp * LONG_RANGE_MULTIPLIER_3);
        } else if (distance > LONG_RANGE_TIER_2) {
            baseXp = (int) (baseXp * LONG_RANGE_MULTIPLIER_2);
        } else if (distance > LONG_RANGE_TIER_1) {
            baseXp = (int) (baseXp * LONG_RANGE_MULTIPLIER_1);
        }

        return new SkillReceptorResult(true, baseXp);
    }

    /**
     * Calcula XP por matar um alvo com flecha
     * 
     * @param target   O alvo morto
     * @param distance Distância do tiro
     * @return SkillReceptorResult com o XP ganho
     */
    public static SkillReceptorResult getArrowKillXp(Entity target, double distance) {
        // Não dá XP para kills em jogadores
        if (target instanceof PlayerEntity) {
            return new SkillReceptorResult(false, 0);
        }

        // Só dá XP para entidades vivas
        if (!(target instanceof LivingEntity)) {
            return new SkillReceptorResult(false, 0);
        }

        int baseXp = XP_KILL_BASE;

        // Bônus de XP para mobs hostis
        if (target instanceof HostileEntity) {
            baseXp = XP_KILL_HOSTILE;
        }

        // Bônus de XP para tiros longos
        if (distance > LONG_RANGE_TIER_3) {
            baseXp = (int) (baseXp * LONG_RANGE_MULTIPLIER_3);
        } else if (distance > LONG_RANGE_TIER_2) {
            baseXp = (int) (baseXp * LONG_RANGE_MULTIPLIER_2);
        } else if (distance > LONG_RANGE_TIER_1) {
            baseXp = (int) (baseXp * LONG_RANGE_MULTIPLIER_1);
        }

        return new SkillReceptorResult(true, baseXp);
    }
}
