package com.murilloskills.utils;

import com.murilloskills.models.SkillReceptorResult;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;

public class WarriorXpGetter {

    public static SkillReceptorResult getMobXp(LivingEntity entity) {

        // BOSSES
        if (entity.getType() == EntityType.ENDER_DRAGON) return new SkillReceptorResult(true, 1000);
        if (entity.getType() == EntityType.WITHER) return new SkillReceptorResult(true, 500);
        if (entity.getType() == EntityType.WARDEN) return new SkillReceptorResult(true, 500);

        // MONSTROS COMUNS (Zumbi, Esqueleto, Creeper, etc)
        if (entity instanceof Monster) {
            if (entity.getType() == EntityType.ENDERMAN) return new SkillReceptorResult(true, 1);
            if (entity.getType() == EntityType.BLAZE) return new SkillReceptorResult(true, 1);

            // Padrão para monstros
            return new SkillReceptorResult(true, 700000);
        }

        // ANIMAIS (Para a skill Hunter)
        if (entity instanceof AnimalEntity) {
            return new SkillReceptorResult(false, 0);
        }

        // Se não for nada disso (ex: Armor Stand, Aldeão)
        return new SkillReceptorResult(false, 0);
    }

}
