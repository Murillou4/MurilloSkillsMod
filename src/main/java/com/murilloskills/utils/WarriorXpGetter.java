package com.murilloskills.utils;

import com.murilloskills.models.SkillReceptorResult;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;

public class WarriorXpGetter {

    // Base XP values for different mob types
    private static final int XP_ENDER_DRAGON = 1000;
    private static final int XP_WITHER = 500;
    private static final int XP_WARDEN = 500;
    private static final int XP_ENDERMAN = 20;
    private static final int XP_BLAZE = 25;
    private static final int XP_MONSTER_DEFAULT = 15;

    public static SkillReceptorResult getMobXp(LivingEntity entity) {

        // BOSSES
        if (entity.getType() == EntityType.ENDER_DRAGON)
            return new SkillReceptorResult(true, XP_ENDER_DRAGON);
        if (entity.getType() == EntityType.WITHER)
            return new SkillReceptorResult(true, XP_WITHER);
        if (entity.getType() == EntityType.WARDEN)
            return new SkillReceptorResult(true, XP_WARDEN);

        // MONSTROS COMUNS (Zumbi, Esqueleto, Creeper, etc)
        if (entity instanceof Monster) {
            if (entity.getType() == EntityType.ENDERMAN)
                return new SkillReceptorResult(true, XP_ENDERMAN);
            if (entity.getType() == EntityType.BLAZE)
                return new SkillReceptorResult(true, XP_BLAZE);

            // Padrão para monstros
            return new SkillReceptorResult(true, XP_MONSTER_DEFAULT);
        }

        // ANIMAIS (Para a skill Hunter)
        if (entity instanceof AnimalEntity) {
            return new SkillReceptorResult(false, 0);
        }

        // Se não for nada disso (ex: Armor Stand, Aldeão)
        return new SkillReceptorResult(false, 0);
    }

}
