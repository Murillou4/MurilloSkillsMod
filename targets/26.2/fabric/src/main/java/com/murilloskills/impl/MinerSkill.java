package com.murilloskills.impl;
import com.murilloskills.api.*;
import com.murilloskills.skills.*;
import com.murilloskills.skills.effects.*;
import com.murilloskills.utils.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.*;
import java.util.*;
import static com.murilloskills.skills.effects.EffectDefinition.of;
import static com.murilloskills.skills.effects.EffectDefinition.Unit.*;

public final class MinerSkill extends AbstractSkill {
    public static final EffectDefinition SPEED=of("miner.speed","Velocidade de mineração",PERCENT,5,null,"Ao minerar; a ativa amplia o bônus.",-1,e->0.10+0.40*e.scale()+e.rank(2)*0.10+(e.active()?0.25*e.tier():0));
    public static final EffectDefinition TOOL_SAVE=of("miner.tool_save","Chance de conservar a ferramenta",PERCENT,30,null,"Recursos naturais; preserva 1 de durabilidade.",0.40,e->0.10+e.rank(2)*0.05);
    public static final EffectDefinition FORTUNE=of("miner.fortune","Chance de recurso extra",PERCENT,45,null,"Minérios, glowstone, argila e cristais elegíveis.",0.40,e->0.15*e.scale()+e.rank(1)*0.05+e.synergy(SynergyService.Effect.ORE));
    public static final EffectDefinition AREA=of("miner.area","Capacidade do Ultmine",BLOCKS,15,null,"Por operação; dobra durante o Pulso.",-1,e->EffectNumbers.area(e,2));
    public static final EffectDefinition PULSE=of("miner.pulse","Raio do pulso de prospecção",BLOCKS,20,null,"Revela minérios em chunks já carregados.",24,e->EffectNumbers.scanRadius(8+e.tier()*2+e.rank(1)*2,e.synergy(SynergyService.Effect.FINDS),e.paragon()?4:0)).onlyWhileActive();
    public static final EffectDefinition RADAR=of("miner.radar","Raio do radar sonoro",BLOCKS,60,Feature.ORE_RADAR,"Verifica a cada 2 s; avisa ao encontrar um alvo ou se aproximar.",6,e->6);
    public static final EffectDefinition FUSION=of("miner.fusion","Fusão de recursos brutos",STATE,65,Feature.MELTING,"Ferro, cobre e ouro viram lingotes.",1,e->1);
    public static final EffectDefinition TORCH=of("miner.torch","Intervalo das tochas",SECONDS,10,Feature.AUTO_TORCH,"Consome tochas em locais escuros.",-1,e->2);
    public java.util.List<EffectDefinition> effects(){return List.of(SPEED,TOOL_SAVE,FORTUNE,AREA,PULSE,RADAR,FUSION,TORCH);}
    public SkillType type(){return SkillType.MINER;}
    public void activate(SkillContext c){begin(c);ScanService.scan(c,false,(int)PULSE.value(c,type()));}
    public void updateAttributes(SkillContext c){Attributes.set(c,net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_BREAK_SPEED,"miner/speed",SPEED.value(c,type()),Operation.ADD_MULTIPLIED_TOTAL);}
    public void tick(SkillContext c){
        if(c.perk(SkillPerk.MINER_VISION)&&c.feature(Feature.NIGHT_VISION))OwnedEffects.grant(c,type(),Feature.NIGHT_VISION,net.minecraft.world.effect.MobEffects.NIGHT_VISION,240,0);
        if(c.perk(SkillPerk.MINER_VISION)&&c.feature(Feature.AUTO_TORCH)&&c.now()%40==0)WorldTools.autoTorch(c);
        if(c.active(type())&&c.now()%60==0)ScanService.scan(c,false,(int)PULSE.value(c,type()));
        if(c.has(type(),60)&&c.feature(Feature.ORE_RADAR)&&c.now()>=c.runtime().nextRadarTick){
            c.runtime().nextRadarTick=c.now()+40;
            var found=WorldTools.loadedBox(c,c.player().blockPosition(),(int)RADAR.value(c,type()),2200).stream().filter(pos->{
                var state=c.player().level().getBlockState(pos);String id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                return WorldTools.ore(state)&&(c.profile().tools.oreFilter.isEmpty()||c.profile().tools.oreFilter.contains(id));
            }).min(java.util.Comparator.comparingDouble(pos->pos.distSqr(c.player().blockPosition()))).orElse(null);
            if(found==null){c.runtime().lastRadarOre=null;c.runtime().lastRadarDistance=Double.MAX_VALUE;}
            else{
                double distance=Math.sqrt(found.distSqr(c.player().blockPosition()));
                if(!found.equals(c.runtime().lastRadarOre)||distance+1<c.runtime().lastRadarDistance){
                    com.murilloskills.network.Wire.notice(c,"radar","radar","Radar sonoro","Recurso próximo · "+Math.round(distance)+" m",1,40);
                    c.runtime().lastRadarOre=found;c.runtime().lastRadarDistance=distance;
                }
            }
        }
    }
    public void broken(SkillContext c,BlockPos pos,BlockState state,boolean natural){
        if(!natural||state.getBlock() instanceof CropBlock)return;
        if(WorldTools.ore(state))c.xp(WorldTools.rareOre(state)?"rare_ore":"ore",1);
        else if(state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE))c.xp("stone",1);
        var tool=c.player().getMainHandItem();
        if(tool.isDamaged()&&c.player().getRandom().nextDouble()<TOOL_SAVE.value(c,type()))tool.setDamageValue(tool.getDamageValue()-1);
    }
    public void drops(SkillContext c,BlockState state,List<ItemStack> drops){
        if(!WorldTools.fortuneResource(state))return;
        for(var stack:drops)if(c.player().getRandom().nextDouble()<FORTUNE.value(c,type()))stack.grow(1);
        if(c.feature(Feature.MELTING)&&c.has(type(),65))for(int i=0;i<drops.size();i++){
            var s=drops.get(i);Item item=s.is(Items.RAW_IRON)?Items.IRON_INGOT:s.is(Items.RAW_GOLD)?Items.GOLD_INGOT:s.is(Items.RAW_COPPER)?Items.COPPER_INGOT:null;
            if(item!=null)drops.set(i,new ItemStack(item,s.getCount()));
        }
    }
    public void cancel(SkillContext c){super.cancel(c);ScanService.cancel(c,"ore");}
    public void clear(SkillContext c){super.clear(c);Attributes.clear(c,"miner/");}
}

