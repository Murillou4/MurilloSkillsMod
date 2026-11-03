package com.murilloskills.impl;
import com.murilloskills.api.*;
import com.murilloskills.skills.*;
import com.murilloskills.skills.effects.*;
import com.murilloskills.utils.OwnedEffects;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import java.util.*;
import static com.murilloskills.skills.effects.EffectDefinition.of;
import static com.murilloskills.skills.effects.EffectDefinition.Unit.*;

public final class FisherSkill extends AbstractSkill {
    public static final EffectDefinition WAIT=of("fisher.wait","Redução da espera",PERCENT,5,null,"Afeta a espera pela fisgada, não uma chance de tesouro.",0.75,e->0.05+0.20*e.scale()+(e.has(10)?0.10:0)+e.rank(1)*0.08+(e.active()?0.20:0));
    public static final EffectDefinition LUCK=of("fisher.luck","Sorte aplicada à pesca",LEVEL,20,null,"Pontos de sorte da tabela vanilla; não é porcentagem.",-1,e->(e.has(30)?1:0)+e.rank(2)+(e.active()?1:0)+(e.paragon()?1:0));
    public static final EffectDefinition BUNDLE=of("fisher.bundle","Chance de peixe adicional",PERCENT,45,null,"Adiciona um peixe por pilha de peixes da captura.",0.50,e->0.20*e.scale()+e.rank(1)*0.08+e.synergy(SynergyService.Effect.DROPS));
    public static final EffectDefinition SAVE=of("fisher.rod","Chance de poupar a vara",PERCENT,65,null,"Reduz em 1 o desgaste real ao recolher a linha.",0.20,e->0.20);
    public static final EffectDefinition VISION=of("fisher.vision","Visão subaquática",STATE,35,Feature.NIGHT_VISION,"Visão noturna somente enquanto estiver na água.",1,e->1).when(SkillEvaluation::water);
    public static final EffectDefinition AIR=of("fisher.air","Respiração aquática",STATE,15,Feature.BREATH,"Efeito vanilla durante a submersão.",1,e->1).when(SkillEvaluation::water);
    public static final EffectDefinition SWIM=of("fisher.swim","Graça dos golfinhos I",STATE,35,Feature.SWIM,"Efeito vanilla de deslocamento na água.",1,e->1).when(SkillEvaluation::water);
    public static final EffectDefinition SEA=of("fisher.sea","Sorte dos mares",LEVEL,55,null,"Efeito de Sorte vanilla; separado dos pontos aplicados ao anzol.",2,e->1+Math.min(1,e.rank(2)));
    public java.util.List<EffectDefinition> effects(){return List.of(WAIT,LUCK,BUNDLE,SAVE,VISION,AIR,SWIM,SEA);}
    public SkillType type(){return SkillType.FISHER;}
    public int rodDamage(SkillContext c,int damage){return damage>0&&c.player().getRandom().nextDouble()<SAVE.value(c,type())?damage-1:damage;}
    public void activate(SkillContext c){begin(c);}
    public void fastTick(SkillContext c){
        if(!c.player().isInWater())for(var feature:List.of(Feature.BREATH,Feature.SWIM,Feature.NIGHT_VISION))OwnedEffects.clearSource(c,type(),feature);
    }
    public void tick(SkillContext c){
        if(AIR.value(c,type())>0)OwnedEffects.grant(c,type(),Feature.BREATH,MobEffects.WATER_BREATHING,60,0);
        if(SWIM.value(c,type())>0)OwnedEffects.grant(c,type(),Feature.SWIM,MobEffects.DOLPHINS_GRACE,60,0);
        if(VISION.value(c,type())>0)OwnedEffects.grant(c,type(),Feature.NIGHT_VISION,MobEffects.NIGHT_VISION,240,0);
        if(SEA.value(c,type())>0)OwnedEffects.grant(c,type(),MobEffects.LUCK,60,(int)SEA.value(c,type())-1);
        if(c.active(type()))c.player().level().sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,c.player().getX(),c.player().getY()+1,c.player().getZ(),4,0.6,0.5,0.6,0.01);
    }
    public double waitReduction(SkillContext c){return WAIT.value(c,type());}
    public int luck(SkillContext c){return (int)LUCK.value(c,type());}
    public void caught(SkillContext c,List<ItemStack> loot){
        if(loot.isEmpty())return;
        boolean treasure=loot.stream().anyMatch(s->s.is(Items.ENCHANTED_BOOK)||s.is(Items.NAME_TAG)||s.is(Items.NAUTILUS_SHELL)||s.is(Items.SADDLE));
        c.xp(treasure?"treasure":"fish",1);
        if(c.player().getRandom().nextDouble()<BUNDLE.value(c,type()))for(var stack:loot)if(stack.is(net.minecraft.tags.ItemTags.FISHES))stack.grow(1);
    }
}

