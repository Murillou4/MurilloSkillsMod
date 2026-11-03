package com.murilloskills.impl;
import com.murilloskills.api.*;
import com.murilloskills.skills.*;
import com.murilloskills.skills.effects.*;
import com.murilloskills.utils.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.damagesource.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.*;
import net.minecraft.world.effect.*;
import java.util.*;
import static com.murilloskills.skills.effects.EffectDefinition.of;
import static com.murilloskills.skills.effects.EffectDefinition.Unit.*;

public final class BlacksmithSkill extends AbstractSkill {
    public static final float REFLECT_RATIO=.25f;
    public static final EffectDefinition XP=of("smith.xp","Experiência de forja",MULTIPLIER,5,null,"Fabricação, fundição e serviços reais.",-1,e->1+e.level()*0.005);
    public static final EffectDefinition SKIN=of("smith.skin","Proteção física da profissão",PERCENT,10,null,"Sujeita ao limite compartilhado de redução de dano.",0.25,e->0.03+0.05*e.scale()+e.rank(2)*0.02+(e.active()?0.10:0));
    public static final EffectDefinition FIRE=of("smith.fire","Proteção adicional a fogo/explosão",PERCENT,35,null,"Soma à proteção da profissão, respeitando os limites.",0.08,e->0.08);
    public static final EffectDefinition FURNACE=of("smith.furnace","Ritmo das fornalhas",MULTIPLIER,15,Feature.FURNACE,"Fornalhas, altos-fornos e defumadores a até 8 blocos.",-1,e->1+0.75*e.scale()+e.rank(2)*0.25);
    public static final EffectDefinition ANVIL=of("smith.anvil","Desconto de bigorna",PERCENT,25,null,"Artesão desde o nível 25; custo final é arredondado para cima e nunca inferior a 1.",0.40,e->(e.has(30)?0.15*e.scale():0)+e.rank(1)*0.05);
    public static final EffectDefinition REPAIR=of("smith.repair","Durabilidade reparada por item",DURABILITY,45,Feature.REPAIR,"A cada 10 s nos itens em mãos e equipados, sem duplicar o mesmo item.",-1,e->1+e.rank(1));
    public static final EffectDefinition ENCHANT=of("smith.enchant","Níveis poupados na mesa",LEVELS,55,null,"Até este valor; custo final mínimo de 1 nível. Artesão III poupa mais um somente no custo 3.",2,e->1+(e.rank(1)>=3?1:0));
    public static final EffectDefinition LIMIT=of("smith.overenchant","Níveis acima do máximo vanilla",LEVEL,65,null,"Combina dois encantamentos máximos iguais. Encantos de nível único são excluídos.",2,e->1+(e.paragon()?1:0));
    public static final EffectDefinition KNOCK=of("smith.knock","Resistência a empurrão",PERCENT,75,null,"A Aura amplia a resistência; fora dela, mantém o bônus passivo.",1,e->e.active()?1:0.5);
    public static final EffectDefinition REFLECT=of("smith.reflect","Chance de refletir um ataque",PERCENT,75,Feature.REFLECT,"Devolve "+EffectDefinition.format(PERCENT,REFLECT_RATIO)+" do menor valor entre dano sofrido e sua vida máxima; somente ataques diretos, sem XP ou roubo de vida.",0.20,e->0.20);
    public static final EffectDefinition AURA=of("smith.aura","Cura da Aura de titânio",HEARTS,20,null,"A cada 2 s enquanto a habilidade estiver ativa.",-1,e->e.paragon()?1.5:0.5).onlyWhileActive();
    public static final EffectDefinition MATERIAL=of("smith.material","Chance de poupar material de reparo",PERCENT,30,null,"Só devolve um material realmente consumido na bigorna.",0.10,e->0.10);
    public java.util.List<EffectDefinition> effects(){return List.of(XP,SKIN,FIRE,FURNACE,ANVIL,REPAIR,ENCHANT,LIMIT,KNOCK,REFLECT,AURA,MATERIAL);}
    public SkillType type(){return SkillType.BLACKSMITH;}
    public void activate(SkillContext c){begin(c);}
    public void tick(SkillContext c){
        if(REPAIR.value(c,type())>0&&c.now()%200==0){
            Set<ItemStack> unique=Collections.newSetFromMap(new IdentityHashMap<>());
            for(var slot:net.minecraft.world.entity.EquipmentSlot.values())unique.add(c.player().getItemBySlot(slot));
            int repaired=0,items=0;
            for(var stack:unique)if(stack.isDamaged()){int before=stack.getDamageValue();stack.setDamageValue(Math.max(0,before-(int)REPAIR.value(c,type())));repaired+=before-stack.getDamageValue();items++;}
            com.murilloskills.network.Wire.notice(c,"smith:repair","repair","Reparo automático",items>0?repaired+" de durabilidade em "+items+" item(ns)":"Nenhum equipamento danificado",items,60);
        }
        if(c.feature(Feature.FIRE_WARD)&&c.has(type(),35))OwnedEffects.grant(c,type(),Feature.FIRE_WARD,MobEffects.FIRE_RESISTANCE,60,0);
        if(c.active(type())&&c.now()%40==0)com.murilloskills.network.Wire.heal(c,"Aura de titânio",(float)AURA.value(c,type()));
    }
    public void updateAttributes(SkillContext c){Attributes.set(c,net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE,"smith/knock",KNOCK.value(c,type()),Operation.ADD_VALUE);}
    public float incoming(SkillContext c,DamageSource source,float damage){
        double reduction=SKIN.value(c,type());
        if(source.is(DamageTypeTags.IS_FIRE)||source.is(DamageTypeTags.IS_EXPLOSION))reduction+=FIRE.value(c,type());
        return damage*(float)(1-Math.min(0.25,reduction));
    }
    public void reflect(SkillContext c,DamageSource source,float taken){
        if(c.runtime().reflecting||taken<=0||source.is(DamageTypes.THORNS)||!(source.getDirectEntity() instanceof net.minecraft.world.entity.LivingEntity attacker)||attacker==c.player())return;
        if(c.player().getRandom().nextDouble()>=REFLECT.value(c,type()))return;
        c.runtime().reflecting=true;
        try{attacker.hurtServer(c.player().level(),c.player().damageSources().thorns(c.player()),Math.min(taken,c.player().getMaxHealth())*REFLECT_RATIO);}
        finally{c.runtime().reflecting=false;}
    }
    public double furnaceMultiplier(SkillContext c){return Math.max(1,FURNACE.value(c,type()));}
    public double discount(SkillContext c){return ANVIL.value(c,type());}
    public int enchantingCost(SkillContext c,int cost){return !c.has(type(),55)?cost:Math.max(1,cost-1-(c.branch(type(),1)>=3&&cost>2?1:0));}
    public int enchantLimit(SkillContext c){return (int)LIMIT.value(c,type());}
    public void forged(SkillContext c,boolean smelt,int amount){c.xp(smelt?"smelt":"forge",smelt?amount:1);}
    public void clear(SkillContext c){super.clear(c);Attributes.clear(c,"smith/");}
}

