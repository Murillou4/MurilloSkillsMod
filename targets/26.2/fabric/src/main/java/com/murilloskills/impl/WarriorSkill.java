package com.murilloskills.impl;
import com.murilloskills.api.*;
import com.murilloskills.skills.*;
import com.murilloskills.skills.effects.*;
import com.murilloskills.utils.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.damagesource.*;
import java.util.*;
import static com.murilloskills.skills.effects.EffectDefinition.of;
import static com.murilloskills.skills.effects.EffectDefinition.Unit.*;

public final class WarriorSkill extends AbstractSkill {
    public static final EffectDefinition HEALTH=of("warrior.health","Vida máxima adicional",HEARTS,10,null,"O bônus por nível volta a crescer após prestigiar.",-1,e->2+2*e.scale()+e.rank(1)+(e.has(65)?1:0));
    public static final EffectDefinition DAMAGE=of("warrior.damage","Dano corpo a corpo",PERCENT,5,null,"Antes do limite compartilhado de dano.",-1,e->0.06+0.19*e.scale()+e.rank(2)*0.04+(e.has(45)?0.03:0)+(e.active()?0.12*e.tier():0));
    public static final EffectDefinition GUARD=of("warrior.guard","Proteção da profissão",PERCENT,15,null,"Combina com outras defesas até o limite do servidor.",-1,e->0.08*e.scale()+e.rank(1)*0.03+(e.has(30)?0.03:0)+(e.active()?0.02*e.tier():0));
    public static final EffectDefinition LEECH=of("warrior.leech","Roubo de vida",PERCENT,55,null,"Apenas dano real em hostis; máximo de 0,5 coração por segundo.",-1,e->0.03+e.rank(2)*0.01);
    public static final EffectDefinition RECOVER=of("warrior.recover","Cura por vitória",HEARTS,35,null,"Ao derrotar um inimigo em combate corpo a corpo.",2,e->0.5+e.rank(2)*0.25);
    public static final EffectDefinition FURY_SPEED=of("warrior.fury_speed","Deslocamento durante Fúria",PERCENT,20,Feature.SPEED,"Somente enquanto a ativa estiver em uso.",0.25,e->0.025*e.tier()).onlyWhileActive();
    public static final EffectDefinition FURY_KNOCK=of("warrior.fury_knock","Resistência a empurrão na Fúria",PERCENT,20,null,"Somente enquanto a ativa estiver em uso.",1,e->e.tier()*0.20).onlyWhileActive();
    public static final EffectDefinition PARAGON=of("warrior.paragon","Cura ao iniciar Fúria",HEARTS,100,null,"Exclusivo do Paragon; ocorre uma vez ao ativar.",4,e->e.paragon()?4:0).onlyWhileActive();
    public java.util.List<EffectDefinition> effects(){return List.of(HEALTH,DAMAGE,GUARD,LEECH,RECOVER,FURY_SPEED,FURY_KNOCK,PARAGON);}
    public SkillType type(){return SkillType.WARRIOR;}
    public void activate(SkillContext c){begin(c);com.murilloskills.network.Wire.heal(c,"Fúria Paragon",(float)PARAGON.value(c,type()));}
    public void updateAttributes(SkillContext c){
        Attributes.set(c,net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,"warrior/health",HEALTH.value(c,type()),Operation.ADD_VALUE);
        Attributes.set(c,net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,"warrior/speed",FURY_SPEED.value(c,type()),Operation.ADD_MULTIPLIED_TOTAL);
        Attributes.set(c,net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE,"warrior/knock",FURY_KNOCK.value(c,type()),Operation.ADD_VALUE);
    }
    public float outgoing(SkillContext c,LivingEntity victim,DamageSource source,float damage){
        if(source.getDirectEntity() instanceof Projectile||source.is(DamageTypes.THORNS))return damage;
        return damage*(float)(1+DAMAGE.value(c,type()));
    }
    public float incoming(SkillContext c,DamageSource source,float damage){return damage*(float)(1-GUARD.value(c,type()));}
    public void leech(SkillContext c,float actualDamage){
        double ratio=LEECH.value(c,type());
        if(ratio>0&&actualDamage>0&&c.now()>=c.runtime().lastLeechTick+20){com.murilloskills.network.Wire.heal(c,"Roubo de vida",Math.min(1,actualDamage*(float)ratio));c.runtime().lastLeechTick=c.now();}
    }
    public void killed(SkillContext c,LivingEntity victim,DamageSource source){
        if(!(victim instanceof Enemy)||source.getDirectEntity() instanceof Projectile||source.is(DamageTypes.THORNS))return;
        c.xp("melee_kill",Math.clamp(victim.getMaxHealth()/20d,0.5,4));com.murilloskills.network.Wire.heal(c,"Recuperação",(float)RECOVER.value(c,type()));
    }
    public void clear(SkillContext c){super.clear(c);Attributes.clear(c,"warrior/");}
}

