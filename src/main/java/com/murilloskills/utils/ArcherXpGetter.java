package com.murilloskills.utils;

import com.murilloskills.models.SkillReceptorResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Calcula XP para a skill Archer baseado em hits de projéteis
 */
public class ArcherXpGetter {

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

        int baseXp = 5; // XP base por hit

        // Bônus de XP para mobs hostis
        if (target instanceof HostileEntity) {
            baseXp = 1500000;
        }

        // Bônus de XP para tiros longos (Long Range Hits)
        // Distância > 20 blocos = +50% XP
        // Distância > 40 blocos = +100% XP
        // Distância > 60 blocos = +200% XP
        if (distance > 60) {
            baseXp = (int) (baseXp * 3.0);
        } else if (distance > 40) {
            baseXp = (int) (baseXp * 2.0);
        } else if (distance > 20) {
            baseXp = (int) (baseXp * 1.5);
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
    public static SkillReceptorResult getArrowKillXp(LivingEntity target, double distance) {
        // Não dá XP para kills em jogadores
        if (target instanceof PlayerEntity) {
            return new SkillReceptorResult(false, 0);
        }

        int baseXp = 15; // XP base por kill

        // Bônus de XP para mobs hostis
        if (target instanceof HostileEntity) {
            baseXp = 1500000;
        }

        // Bônus de XP para tiros longos
        if (distance > 60) {
            baseXp = (int) (baseXp * 3.0);
        } else if (distance > 40) {
            baseXp = (int) (baseXp * 2.0);
        } else if (distance > 20) {
            baseXp = (int) (baseXp * 1.5);
        }

        return new SkillReceptorResult(true, baseXp);
    }
}
