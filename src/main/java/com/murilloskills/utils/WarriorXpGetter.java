package com.murilloskills.utils;

import com.murilloskills.models.SkillReceptorResult;
import com.murilloskills.core.compat.CrossModCompatRules;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.registry.Registries;

public class WarriorXpGetter {

    // Base XP values for different mob types
    // Note: Constants replaced by SkillConfig getters

    public static SkillReceptorResult getMobXp(LivingEntity entity) {

        // BOSSES
        if (entity.getType() == EntityType.ENDER_DRAGON)
            return new SkillReceptorResult(true, SkillConfig.getWarriorXpEnderDragon());
        if (entity.getType() == EntityType.WITHER)
            return new SkillReceptorResult(true, SkillConfig.getWarriorXpWither());
        if (entity.getType() == EntityType.WARDEN)
            return new SkillReceptorResult(true, SkillConfig.getWarriorXpWarden());

        // MONSTROS COMUNS (Zumbi, Esqueleto, Creeper, etc)
        if (entity instanceof Monster) {
            if (entity.getType() == EntityType.ENDERMAN)
                return new SkillReceptorResult(true, SkillConfig.getWarriorXpEnderman());
            if (entity.getType() == EntityType.BLAZE)
                return new SkillReceptorResult(true, SkillConfig.getWarriorXpBlaze());

            // Padrão para monstros
            return new SkillReceptorResult(true, SkillConfig.getWarriorXpMonsterDefault());
        }

        // ANIMAIS (Para a skill Hunter)
        if (entity instanceof AnimalEntity) {
            return new SkillReceptorResult(false, 0);
        }

        // Se não for nada disso (ex: Armor Stand, Aldeão)
        String entityId = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
        if (CrossModCompatRules.isLikelyHostileEntityId(entityId)
                || CrossModCompatRules.isLikelyHostileEntityId(entity.getClass().getName())) {
            return new SkillReceptorResult(true, SkillConfig.getWarriorXpMonsterDefault());
        }

        return new SkillReceptorResult(false, 0);
    }

}
