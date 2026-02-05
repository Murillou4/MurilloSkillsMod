package com.murilloskills.utils;

import com.murilloskills.data.XpDataManager;
import com.murilloskills.models.SkillReceptorResult;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;

public class WarriorXpGetter {

    // Base XP values for different mob types
    // Note: Constants replaced by SkillConfig getters

    public static SkillReceptorResult getMobXp(LivingEntity entity) {
        int overrideXp = XpDataManager.getEntityXp("warrior", entity.getType());
        if (overrideXp > 0) {
            return new SkillReceptorResult(true, overrideXp);
        }

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
        return new SkillReceptorResult(false, 0);
    }

}
