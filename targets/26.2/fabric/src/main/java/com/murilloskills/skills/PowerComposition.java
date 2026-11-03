package com.murilloskills.skills;
import com.murilloskills.api.SkillContext;
import com.murilloskills.impl.*;
/** The caps used by gameplay and the combined character presentation. Values preserved from 2.1. */
public final class PowerComposition {
    public static final float ACTIVE_DAMAGE_MULTIPLIER=1.75f;
    private PowerComposition(){}
    public static float capDamage(float original,float modified,boolean active,double normalCap){return Math.min(modified,original*(active?ACTIVE_DAMAGE_MULTIPLIER:(float)(1+normalCap)));}
    public static float capResistance(float original,float modified,double cap){return Math.max(modified,original*(float)(1-cap));}
    public static double capDamage(double original,double modified,boolean active,double normalCap){return Math.min(modified,original*(active?ACTIVE_DAMAGE_MULTIPLIER:1+normalCap));}
    public static double capResistance(double original,double modified,double cap){return Math.max(modified,original*(1-cap));}
    public static double movement(double factor,double cap){return Math.min(factor,1+cap)-1;}
    public static double damageBonus(SkillContext c,boolean ranged,boolean head){
        double contribution=ranged?ArcherSkill.DAMAGE.value(c,SkillType.ARCHER)+(head?ArcherSkill.HEADSHOT.value(c,SkillType.ARCHER):0):WarriorSkill.DAMAGE.value(c,SkillType.WARRIOR);
        boolean active=c.runtime().activeUntil.values().stream().anyMatch(t->t>c.now());
        return capDamage(1,(1+contribution)*(1+SynergyService.bonus(c.profile(),SynergyService.Effect.DAMAGE)),active,c.store().config.maxDamageBonus)-1;
    }
    public static double defense(SkillContext c){
        double after=(1-WarriorSkill.GUARD.value(c,SkillType.WARRIOR))*(1-BlacksmithSkill.SKIN.value(c,SkillType.BLACKSMITH))*(1-SynergyService.bonus(c.profile(),SynergyService.Effect.DEFENSE));
        return 1-capResistance(1,after,c.store().config.maxSkillResistance);
    }
    public static double speed(SkillContext c){
        double factor=(1+WarriorSkill.FURY_SPEED.value(c,SkillType.WARRIOR))*(1+ArcherSkill.DRAW.value(c,SkillType.ARCHER))*(1+ExplorerSkill.SPEED.value(c,SkillType.EXPLORER))*(1+SynergyService.bonus(c.profile(),SynergyService.Effect.SPEED));
        return movement(factor,c.store().config.maxMoveBonus);
    }
}
