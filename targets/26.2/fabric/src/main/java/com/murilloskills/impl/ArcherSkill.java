package com.murilloskills.impl;
import com.murilloskills.api.*;
import com.murilloskills.skills.*;
import com.murilloskills.skills.effects.*;
import com.murilloskills.utils.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.damagesource.*;
import java.util.*;
import static com.murilloskills.skills.effects.EffectDefinition.of;
import static com.murilloskills.skills.effects.EffectDefinition.Unit.*;

public final class ArcherSkill extends AbstractSkill {
    public static final EffectDefinition DAMAGE=of("archer.damage","Dano de projéteis",PERCENT,5,null,"Antes do limite compartilhado; headshot é separado.",-1,e->0.05+0.20*e.scale()+(e.active()?0.10*e.tier():0));
    public static final EffectDefinition SPEED=of("archer.speed","Velocidade das flechas",MULTIPLIER,10,null,"Multiplicador da velocidade inicial.",-1,e->1.10+0.05*e.scale()+e.rank(2)*0.05+(e.active()?0.10:0));
    public static final EffectDefinition PRECISION=of("archer.precision","Redução da dispersão",PERCENT,15,null,"Precisão melhora desde o primeiro patamar do ramo.",0.70,e->1-Math.max(0.30,(e.has(65)?0.45:0.65)-e.rank(1)*0.05));
    public static final EffectDefinition HEADSHOT=of("archer.headshot","Dano adicional de headshot",PERCENT,30,null,"Acerto na parte superior do inimigo.",-1,e->0.10+e.rank(1)*0.04+(e.has(65)?0.05:0));
    public static final EffectDefinition PENETRATION=of("archer.penetration","Armadura ignorada",PERCENT,45,null,"Percentual da redução por armadura ignorado.",0.30,e->0.10*e.scale()+e.rank(1)*0.04+(e.paragon()&&e.active()?0.05:0));
    public static final EffectDefinition PIERCE=of("archer.pierce","Inimigos adicionais atravessados",TARGETS,45,null,"Flechas continuam respeitando blocos e colisões.",2,e->e.highest()>=80?2:1);
    public static final EffectDefinition DRAW=of("archer.draw","Movimento ao preparar o arco",PERCENT,35,Feature.SPEED,"Durante o uso de um arco ou besta.",-1,e->0.04+0.06*e.rank(2)).when(SkillEvaluation::drawing);
    public static final EffectDefinition SAVE=of("archer.save","Chance de conservar flecha",PERCENT,55,null,"Só devolve uma flecha realmente consumida.",0.20,e->0.10*e.scale());
    public static final EffectDefinition GUIDED=of("archer.guided","Flechas guiadas do Paragon",STATE,100,null,"Durante Foco; alvos hostis visíveis em até 24 blocos.",1,e->e.paragon()?1:0).onlyWhileActive();
    public java.util.List<EffectDefinition> effects(){return List.of(DAMAGE,SPEED,PRECISION,HEADSHOT,PENETRATION,PIERCE,DRAW,SAVE,GUIDED);}
    public SkillType type(){return SkillType.ARCHER;}
    public float spreadMultiplier(SkillContext c){return (float)(1-PRECISION.value(c,type()));}
    public double arrowConservation(SkillContext c){return SAVE.value(c,type());}
    public void activate(SkillContext c){begin(c);}
    public float outgoing(SkillContext c,LivingEntity victim,DamageSource source,float damage){
        if(!(source.getDirectEntity() instanceof Projectile arrow))return damage;
        double head=headshot(c,victim,arrow)?HEADSHOT.value(c,type()):0;
        return damage*(float)(1+DAMAGE.value(c,type())+head);
    }
    public boolean headshot(SkillContext c,LivingEntity victim,Projectile arrow){return c.runtime().impacts.getOrDefault(arrow.getUUID(),arrow.position()).y>victim.getY()+victim.getBbHeight()*.7;}
    public void killed(SkillContext c,LivingEntity victim,DamageSource source){if(victim instanceof Enemy&&source.getDirectEntity() instanceof Projectile)c.xp("ranged_kill",Math.clamp(victim.getMaxHealth()/20d,0.5,4));}
    public double arrowSpeed(SkillContext c){return Math.max(1,SPEED.value(c,type()));}
    public float armorPenetration(SkillContext c){return (float)PENETRATION.value(c,type());}
    public int piercing(SkillContext c){return (int)PIERCE.value(c,type());}
    public void guide(SkillContext c,AbstractArrow arrow){
        if(GUIDED.value(c,type())==0||arrow.tickCount%5!=0||arrow.getDeltaMovement().lengthSqr()<0.04)return;
        var direction=arrow.getDeltaMovement().normalize();
        var target=c.player().level().getEntitiesOfClass(LivingEntity.class,arrow.getBoundingBox().inflate(24),e->e instanceof Enemy&&e.isAlive()&&c.player().hasLineOfSight(e)
            &&e.getEyePosition().subtract(arrow.position()).normalize().dot(direction)>0.4).stream().min(Comparator.comparingDouble(e->e.distanceToSqr(arrow))).orElse(null);
        if(target!=null){double speed=arrow.getDeltaMovement().length();var desired=target.getEyePosition().subtract(arrow.position()).normalize();
            arrow.setDeltaMovement(direction.scale(0.75).add(desired.scale(0.25)).normalize().scale(speed));}
    }
    public void updateAttributes(SkillContext c){Attributes.set(c,net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,"archer/draw",DRAW.value(c,type()),Operation.ADD_MULTIPLIED_TOTAL);}
    public void clear(SkillContext c){super.clear(c);Attributes.clear(c,"archer/");}
}

