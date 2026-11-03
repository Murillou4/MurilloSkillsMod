package com.murilloskills.impl;
import com.murilloskills.api.*;
import com.murilloskills.skills.*;
import com.murilloskills.skills.effects.*;
import com.murilloskills.utils.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.damagesource.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import java.util.*;

public final class BuilderSkill extends AbstractSkill {
    public static final int DESCENT_MIN_Y=100;
    public static final EffectDefinition REACH=EffectDefinition.of("reach","Alcance adicional",EffectDefinition.Unit.BLOCKS,5,null,"Ao colocar e interagir com blocos",4,e->.25+(e.has(10)?1.5*e.scale():0)+.25*e.rank(1)+(e.has(65)?.5:0));
    public static final EffectDefinition FALL=EffectDefinition.of("fall","Redução de dano de queda",EffectDefinition.Unit.PERCENT,30,null,"Acumula com a redução de queda do Explorador; a queda continua causando dano",.8,e->.2);
    public static final EffectDefinition SAFE=EffectDefinition.of("safe","Distância de queda segura adicional",EffectDefinition.Unit.BLOCKS,30,null,"Antes do cálculo vanilla de queda",5,e->2);
    public static final EffectDefinition CLIMB=EffectDefinition.of("climb","Velocidade vertical adicional",EffectDefinition.Unit.PERCENT,35,Feature.CLIMB,"Subindo escadas e andaimes",.5,e->.15);
    public static final EffectDefinition ECONOMY=EffectDefinition.of("economy","Economia de materiais",EffectDefinition.Unit.PERCENT,45,null,"Somente blocos decorativos não reversíveis; exige materiais",.25,e->.12*e.scale()+.03*e.rank(2)+e.synergy(SynergyService.Effect.ECONOMY)+(e.paragon()&&e.active()?.10:0));
    public static final EffectDefinition AREA=EffectDefinition.of("area","Limite de UltPlace",EffectDefinition.Unit.BLOCKS,15,null,"Cada bloco consome material; dobra durante a ativa até o limite do servidor",-1,e->EffectNumbers.area(e,2));
    public static final EffectDefinition BRUSH_AREA=EffectDefinition.of("brush.area","Limite do Pincel",EffectDefinition.Unit.BLOCKS,20,null,"Volume entre os dois pontos; mantém o limite próprio do Pincel",256,e->EffectNumbers.area(e.withActive(true),2)).onlyWhileActive();
    public static final EffectDefinition CRAFT=EffectDefinition.of("craft","Produção decorativa extra",EffectDefinition.Unit.PERCENT,15,null,"Chance de +1 tijolo, bloco de pedra talhada ou polida por fabricação",.25,e->.05+.15*e.scale()+.02*e.rank(2));
    public static final EffectDefinition VIGOR=EffectDefinition.of("vigor","Vigor construtor",EffectDefinition.Unit.LEVEL,35,Feature.BUILD_HASTE,"Pressa vanilla I enquanto a opção estiver ligada",1,e->1);
    public static final EffectDefinition DESCENT=EffectDefinition.of("descent","Queda lenta",EffectDefinition.Unit.STATE,60,Feature.SLOW_FALL,"Agachado a partir de Y="+DESCENT_MIN_Y,1,e->1);
    public static final EffectDefinition UNDO=EffectDefinition.of("undo","Janela para desfazer",EffectDefinition.Unit.SECONDS,15,null,"Fora de combate, inclusive durante o Pincel; só blocos ainda iguais aos colocados",120,e->60+20*e.rank(1));
    private static final class Decor {static final Set<Item> ITEMS=Set.of(Items.BRICKS,Items.STONE_BRICKS,Items.CUT_SANDSTONE,Items.CUT_RED_SANDSTONE,Items.POLISHED_ANDESITE,Items.POLISHED_DIORITE,Items.POLISHED_GRANITE);}
    public SkillType type(){return SkillType.BUILDER;}
    public List<EffectDefinition> effects(){return List.of(REACH,FALL,SAFE,CLIMB,ECONOMY,AREA,BRUSH_AREA,CRAFT,VIGOR,DESCENT,UNDO);}
    public void activate(SkillContext c){begin(c);BrushTools.clear(c);}
    public void cancel(SkillContext c){super.cancel(c);BrushTools.clear(c);}
    public void updateAttributes(SkillContext c){
        Attributes.set(c,net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE,"builder/reach",REACH.value(c,type()),Operation.ADD_VALUE);
        Attributes.set(c,net.minecraft.world.entity.ai.attributes.Attributes.SAFE_FALL_DISTANCE,"builder/fall",SAFE.value(c,type()),Operation.ADD_VALUE);
    }
    public void fastTick(SkillContext c){if(!c.player().isShiftKeyDown()||c.player().blockPosition().getY()<DESCENT_MIN_Y)OwnedEffects.clearSource(c,type(),Feature.SLOW_FALL);}
    public void tick(SkillContext c){
        if(c.player().onClimbable()&&c.player().getDeltaMovement().y>0)c.player().setDeltaMovement(c.player().getDeltaMovement().multiply(1,1+CLIMB.value(c,type()),1));
        if(VIGOR.value(c,type())>0)OwnedEffects.grant(c,type(),Feature.BUILD_HASTE,MobEffects.HASTE,60,0);
        if(DESCENT.value(c,type())>0&&c.player().isShiftKeyDown()&&c.player().blockPosition().getY()>=DESCENT_MIN_Y)OwnedEffects.grant(c,type(),Feature.SLOW_FALL,MobEffects.SLOW_FALLING,30,0);
        if(c.active(type())&&c.profile().equipped==type())BrushTools.preview(c);
    }
    public void crafted(SkillContext c,ItemStack result){if(c.player().gameMode.isSurvival()&&Decor.ITEMS.contains(result.getItem())&&c.chance(CRAFT.value(c,type())))c.give(new ItemStack(result.getItem(),1));}
    public void placed(SkillContext c,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state,boolean first){if(first&&!(state.getBlock() instanceof net.minecraft.world.level.block.CropBlock))c.xp("place",1);}
    public float incoming(SkillContext c,DamageSource source,float damage){return source.is(DamageTypeTags.IS_FALL)?damage*(float)(1-FALL.value(c,type())):damage;}
    public double economy(SkillContext c){return ECONOMY.value(c,type());}
    public void clear(SkillContext c){super.clear(c);Attributes.clear(c,"builder/");BrushTools.clear(c);}
}

