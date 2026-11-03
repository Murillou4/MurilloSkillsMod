package com.murilloskills.impl;
import com.murilloskills.api.*;
import com.murilloskills.skills.*;
import com.murilloskills.skills.effects.*;
import com.murilloskills.utils.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.*;
import java.util.*;
import static com.murilloskills.skills.effects.EffectDefinition.of;
import static com.murilloskills.skills.effects.EffectDefinition.Unit.*;

public final class FarmerSkill extends AbstractSkill {
    public static final int GROW_INTERVAL=100,GROW_LIMIT=6;
    public static final double GROW_CHANCE=.12;
    public static final EffectDefinition YIELD=of("farmer.yield","Chance de colheita adicional",PERCENT,5,null,"Por pilha de drops de um cultivo maduro.",0.60,e->0.08+0.17*e.scale()+e.rank(2)*0.08+e.synergy(SynergyService.Effect.DROPS));
    public static final EffectDefinition SAVE=of("farmer.seed_save","Chance de conservar sementes",PERCENT,30,null,"Somente após um plantio realmente bem-sucedido.",0.30,e->0.10+e.rank(1)*0.04);
    public static final EffectDefinition PLANT=of("farmer.plant","Raio do plantio",BLOCKS,15,Feature.PLANT,"A área tem largura 2×raio+1 e consome sementes.",4,e->(e.highest()>=75?3:e.has(55)?2:1)+(e.rank(1)>0?1:0));
    public static final EffectDefinition MEAL=of("farmer.meal","Raio do bone meal em área",BLOCKS,25,Feature.BONEMEAL,"Um bone meal por uso vanilla bem-sucedido.",4,e->(e.highest()>=75?3:e.has(55)?2:1)+(e.rank(1)>0?1:0));
    public static final EffectDefinition HARVEST=of("farmer.harvest","Raio da colheita adjacente",BLOCKS,45,Feature.HARVEST,"Apenas plantações maduras; replantio é separado.",2,e->e.has(55)?2:1);
    public static final EffectDefinition GROW=of("farmer.grow","Raio do crescimento automático",BLOCKS,35,Feature.GROWTH,"A cada "+GROW_INTERVAL/20+" s; até "+GROW_LIMIT+" cultivos, chance de "+EffectDefinition.format(PERCENT,GROW_CHANCE)+" por candidato.",8,e->2+e.rank(1));
    public static final EffectDefinition SURGE=of("farmer.surge","Raio do Surto de crescimento",BLOCKS,20,null,"A cada 2 s durante a ativa; cresce 2 estágios, ou 3 como Paragon.",8,e->3+e.tier()).onlyWhileActive();
    public static final EffectDefinition GOLD=of("farmer.gold","Chance de cenoura dourada",PERCENT,65,null,"Um drop adicional em colheitas maduras.",0.025,e->e.paragon()?0.025:0.01);
    public static final EffectDefinition COMPOST=of("farmer.compost","Bone meal por composteira pronta",ITEMS,50,null,"Extração manual; usa o composto vanilla.",2,e->2);
    public static final EffectDefinition FIELD=of("farmer.field","Cura em terreno de cultivo",HEARTS,35,Feature.FARM_REGEN,"0,5 coração a cada 2,5 s sobre grama ou terra arada.",1,e->1);
    public java.util.List<EffectDefinition> effects(){return List.of(YIELD,SAVE,PLANT,MEAL,HARVEST,GROW,SURGE,GOLD,COMPOST,FIELD);}
    public SkillType type(){return SkillType.FARMER;}
    public void cancel(SkillContext c){super.cancel(c);c.runtime().growth.remove("surge");}
    public void clear(SkillContext c){super.clear(c);GrowthService.clear(c);}
    public boolean useBlock(SkillContext c,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        if(hand!=net.minecraft.world.InteractionHand.MAIN_HAND||c.runtime().area)return false;
        var stack=c.player().getMainHandItem();
        if(c.has(type(),25)&&c.feature(Feature.BONEMEAL)&&stack.is(Items.BONE_MEAL))return FarmingTools.boneMeal(c,hit.getBlockPos())>0;
        if(c.has(type(),15)&&c.feature(Feature.PLANT)&&stack.getItem() instanceof BlockItem item&&item.getBlock() instanceof CropBlock)return FarmingTools.plant(c,hit.getBlockPos().above(),item)>0;
        return false;
    }
    public double seedConservation(SkillContext c){return SAVE.value(c,type());}
    public int compostAmount(SkillContext c){return Math.max(1,(int)COMPOST.value(c,type()));}
    public void activate(SkillContext c){begin(c);WorldTools.grow(c,(int)SURGE.value(c,type()),true);}
    public void tick(SkillContext c){
        if(GROW.value(c,type())>0&&c.now()%GROW_INTERVAL==0)WorldTools.grow(c,(int)GROW.value(c,type()),false);
        if(c.active(type())&&c.now()%40==0)WorldTools.grow(c,(int)SURGE.value(c,type()),true);
    }
    public void fastTick(SkillContext c){
        if(FIELD.value(c,type())>0&&c.now()%50==0){var floor=c.player().level().getBlockState(c.player().blockPosition().below());if(floor.is(Blocks.FARMLAND)||floor.is(Blocks.GRASS_BLOCK))com.murilloskills.network.Wire.heal(c,"Vitalidade do campo",(float)FIELD.value(c,type()));}
    }
    public void broken(SkillContext c,BlockPos pos,BlockState state,boolean natural){
        if(state.getBlock() instanceof CropBlock crop&&crop.isMaxAge(state)){
            c.xp("harvest",1);
            if(c.has(type(),10)&&c.feature(Feature.REPLANT))WorldTools.replant(c,pos,crop);
            if(HARVEST.value(c,type())>0&&!c.runtime().area)FarmingTools.harvest(c,pos);
        }else if(state.is(Blocks.NETHER_WART)&&state.getValue(NetherWartBlock.AGE)==3)c.xp("harvest",1);
    }
    public void placed(SkillContext c,BlockPos pos,BlockState state,boolean first){if(state.getBlock() instanceof CropBlock&&first)c.xp("plant",1);}
    public void drops(SkillContext c,BlockState state,List<ItemStack> drops){
        if(!(state.getBlock() instanceof CropBlock crop)||!crop.isMaxAge(state))return;
        for(var stack:drops)if(c.player().getRandom().nextDouble()<YIELD.value(c,type()))stack.grow(1);
        if(c.has(type(),50)&&c.player().getRandom().nextDouble()<0.05)drops.add(new ItemStack(WorldTools.seedFor(crop)));
        if(c.player().getRandom().nextDouble()<GOLD.value(c,type()))drops.add(new ItemStack(Items.GOLDEN_CARROT));
    }
}

