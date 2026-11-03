package com.murilloskills.impl;
import com.murilloskills.api.*;
import com.murilloskills.skills.*;
import com.murilloskills.skills.effects.*;
import com.murilloskills.utils.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.effect.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.tags.DamageTypeTags;
import java.util.*;

public final class ExplorerSkill extends AbstractSkill {
    public static final float FIRE_MULTIPLIER=.8f;
    public static final EffectDefinition SPEED=EffectDefinition.of("speed","Velocidade adicional",EffectDefinition.Unit.PERCENT,5,Feature.SPEED,"Sujeita ao limite compartilhado de movimento configurado pelo servidor",.2,e->.04+.11*e.scale()+.02*e.rank(1));
    public static final EffectDefinition STEP=EffectDefinition.of("step","Altura de passo adicional",EffectDefinition.Unit.BLOCKS,10,Feature.STEP,"Permite subir blocos inteiros",.5,e->.5);
    public static final EffectDefinition HUNGER=EffectDefinition.of("hunger","Redução de exaustão",EffectDefinition.Unit.PERCENT,5,null,"Não elimina a necessidade de comida",.35,e->.05+.15*e.scale()+.04*e.rank(1));
    public static final EffectDefinition FALL=EffectDefinition.of("fall","Redução de dano de queda",EffectDefinition.Unit.PERCENT,45,null,"Acumula com Construtor",.3,e->.3);
    public static final EffectDefinition SAFE=EffectDefinition.of("safe","Distância de queda segura adicional",EffectDefinition.Unit.BLOCKS,45,null,"Antes do cálculo vanilla",3,e->3);
    public static final EffectDefinition BREATH=EffectDefinition.of("breath","Respiração aquática",EffectDefinition.Unit.STATE,15,Feature.BREATH,"Enquanto estiver submerso",1,e->1).when(SkillEvaluation::water);
    public static final EffectDefinition VISION=EffectDefinition.of("vision","Visão noturna",EffectDefinition.Unit.STATE,30,Feature.NIGHT_VISION,"Pode ser desligada a qualquer momento",1,e->1);
    public static final EffectDefinition SCAN=EffectDefinition.of("scan","Raio da Busca de tesouros",EffectDefinition.Unit.BLOCKS,20,null,"Contêineres em chunks carregados; Paragon também sinaliza spawners e cofres",24,e->EffectNumbers.scanRadius(8+2*e.tier()+2*e.rank(2)+(e.has(65)?4:0),e.synergy(SynergyService.Effect.FINDS),0)).onlyWhileActive();
    public static final EffectDefinition RECOVER=EffectDefinition.of("recovery","Recuperação de vida",EffectDefinition.Unit.HEARTS,55,Feature.RECOVERY,"A cada 2,5 s, após 10 s sem combate",2,e->1).when(e->!e.combat());
    public static final EffectDefinition NETHER=EffectDefinition.of("nether","Travessia do Nether",EffectDefinition.Unit.STATE,55,Feature.NETHER,"Evita dano de magma e lentidão da areia das almas; reduz dano de fogo em "+EffectDefinition.format(EffectDefinition.Unit.PERCENT,1-FIRE_MULTIPLIER)+" até o limite compartilhado; resistência ao fogo na lava",1,e->1);
    public SkillType type(){return SkillType.EXPLORER;}
    public List<EffectDefinition> effects(){return List.of(SPEED,STEP,HUNGER,FALL,SAFE,BREATH,VISION,SCAN,RECOVER,NETHER);}
    public boolean useBlock(SkillContext c,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        if(hand==net.minecraft.world.InteractionHand.MAIN_HAND&&c.player().gameMode.isSurvival()&&c.has(type(),5)&&c.player().level().getBlockEntity(hit.getBlockPos()) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity
            &&c.store().natural(c.player(),hit.getBlockPos())&&c.profile().discoveries.add("loot:"+c.store().key(c.player(),hit.getBlockPos())))c.xp("loot",1);
        return false;
    }
    public void activate(SkillContext c){begin(c);ScanService.scan(c,true,(int)SCAN.value(c,type()));}
    public void cancel(SkillContext c){super.cancel(c);ScanService.cancel(c,"treasure");}
    public void updateAttributes(SkillContext c){
        Attributes.set(c,net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,"explorer/speed",SPEED.value(c,type()),Operation.ADD_MULTIPLIED_TOTAL);
        Attributes.set(c,net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT,"explorer/step",STEP.value(c,type()),Operation.ADD_VALUE);
        Attributes.set(c,net.minecraft.world.entity.ai.attributes.Attributes.SAFE_FALL_DISTANCE,"explorer/fall",SAFE.value(c,type()),Operation.ADD_VALUE);
    }
    public void fastTick(SkillContext c){
        if(!c.player().isInWater())OwnedEffects.clearSource(c,type(),Feature.BREATH);
        if(!c.player().isInLava())OwnedEffects.clearSource(c,type(),Feature.NETHER);
        if(c.now()%50==0&&c.runtime().combatUntil<=c.now()&&RECOVER.value(c,type())>0)com.murilloskills.network.Wire.heal(c,"Recuperação",(float)RECOVER.value(c,type()));
    }
    public void tick(SkillContext c){
        if(BREATH.value(c,type())>0&&c.player().isInWater())OwnedEffects.grant(c,type(),Feature.BREATH,MobEffects.WATER_BREATHING,60,0);
        if(VISION.value(c,type())>0)OwnedEffects.grant(c,type(),Feature.NIGHT_VISION,MobEffects.NIGHT_VISION,240,0);
        if(NETHER.value(c,type())>0&&c.player().isInLava())OwnedEffects.grant(c,type(),Feature.NETHER,MobEffects.FIRE_RESISTANCE,60,0);
        var pos=c.player().position();var run=c.runtime();String dimension=c.player().level().dimension().identifier().toString();
        if(run.lastPosition!=null&&run.lastDimension.equals(dimension)&&c.player().onGround()&&!c.player().isPassenger()&&!c.player().isFallFlying()){
            double d=pos.distanceTo(run.lastPosition);if(d>.05&&d<12){String cell="walk:"+dimension+":"+(c.player().blockPosition().getX()>>4)+":"+(c.player().blockPosition().getZ()>>4);run.traveled+=d;if(run.traveled>=20){run.traveled-=20;if(c.profile().discoveries.add(cell))c.xp("travel",1);}}
        }
        run.lastPosition=pos;run.lastDimension=dimension;
        var biome=c.player().level().getBiome(c.player().blockPosition()).unwrapKey();
        if(biome.isPresent()&&c.profile().discoveries.add("biome:"+biome.get().identifier()))c.xp("biome",1);
        for(var structure:c.player().level().structureManager().getAllStructuresAt(c.player().blockPosition()).keySet()){
            var start=c.player().level().structureManager().getStructureWithPieceAt(c.player().blockPosition(),structure);
            if(start.isValid()){var key=c.player().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getKey(structure);if(key!=null&&c.profile().discoveries.add("structure:"+dimension+":"+key+":"+start.getChunkPos().pack()))c.xp("structure",1);}
        }
        if(c.active(type())&&c.now()%80==0)ScanService.scan(c,true,(int)SCAN.value(c,type()));
    }
    public float incoming(SkillContext c,DamageSource source,float damage){
        if(NETHER.value(c,type())>0&&source.is(DamageTypes.HOT_FLOOR))return 0;
        if(source.is(DamageTypeTags.IS_FALL))return damage*(float)(1-FALL.value(c,type()));
        if(NETHER.value(c,type())>0&&source.is(DamageTypeTags.IS_FIRE))return damage*FIRE_MULTIPLIER;
        return damage;
    }
    public float hungerMultiplier(SkillContext c){return (float)(1-HUNGER.value(c,type()));}
    public void clear(SkillContext c){super.clear(c);Attributes.clear(c,"explorer/");c.runtime().lastPosition=null;c.runtime().traveled=0;}
}

