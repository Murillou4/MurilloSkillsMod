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
    // Note: Constants replaced by SkillConfig getters

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

        int baseXp = SkillConfig.getArcherXpHitBase();

        // Bônus de XP para mobs hostis
        if (target instanceof HostileEntity) {
            baseXp = SkillConfig.getArcherXpHitHostile();
        }

        // Bônus de XP para tiros longos (Long Range Hits)
        // Distância > 20 blocos = +50% XP
        // Distância > 40 blocos = +100% XP
        // Distância > 60 blocos = +200% XP
        if (distance > SkillConfig.getArcherLongRangeTier3()) {
            baseXp = (int) (baseXp * SkillConfig.getArcherLongRangeMultiplier3());
        } else if (distance > SkillConfig.getArcherLongRangeTier2()) {
            baseXp = (int) (baseXp * SkillConfig.getArcherLongRangeMultiplier2());
        } else if (distance > SkillConfig.getArcherLongRangeTier1()) {
            baseXp = (int) (baseXp * SkillConfig.getArcherLongRangeMultiplier1());
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

        int baseXp = SkillConfig.getArcherXpKillBase();

        // Bônus de XP para mobs hostis
        if (target instanceof HostileEntity) {
            baseXp = SkillConfig.getArcherXpKillHostile();
        }

        // Bônus de XP para tiros longos
        if (distance > SkillConfig.getArcherLongRangeTier3()) {
            baseXp = (int) (baseXp * SkillConfig.getArcherLongRangeMultiplier3());
        } else if (distance > SkillConfig.getArcherLongRangeTier2()) {
            baseXp = (int) (baseXp * SkillConfig.getArcherLongRangeMultiplier2());
        } else if (distance > SkillConfig.getArcherLongRangeTier1()) {
            baseXp = (int) (baseXp * SkillConfig.getArcherLongRangeMultiplier1());
        }

        return new SkillReceptorResult(true, baseXp);
    }
}
