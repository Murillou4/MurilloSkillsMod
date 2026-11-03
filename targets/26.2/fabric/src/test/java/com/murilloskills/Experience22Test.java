package com.murilloskills;
import com.murilloskills.api.AbstractSkill;
import com.murilloskills.config.ModConfig;
import com.murilloskills.data.*;
import com.murilloskills.impl.*;
import com.murilloskills.skills.*;
import com.murilloskills.skills.effects.*;
import com.murilloskills.utils.ShellPositions;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class Experience22Test {
    @Test void presentationKeepsUnitsPrecisionAndSinglePositiveSign(){
        assertEquals("1,1005×",EffectDefinition.format(EffectDefinition.Unit.MULTIPLIER,1.1005));
        assertEquals("+1",FisherSkill.LUCK.increase(1));
        assertEquals("1 alvo",ArcherSkill.PIERCE.format(1));
        assertEquals("2 níveis",BlacksmithSkill.ENCHANT.format(2));
        assertEquals("1 bloco",MinerSkill.AREA.format(1));
    }
    @Test void toolAndBrushCapacitiesDescribeTheirExistingSeparateLimits(){
        var p=new PlayerProfile();p.selected.add(SkillType.BUILDER);p.stats(SkillType.BUILDER).level=100;p.stats(SkillType.BUILDER).highestLevel=100;
        var config=new ModConfig();config.maxAreaBlocks=512;
        var e=SkillEvaluation.of(p,SkillType.BUILDER,config,true,false,false,false,false);
        assertEquals(306,BuilderSkill.AREA.value(e));assertEquals(256,BuilderSkill.BRUSH_AREA.value(e));
        assertEquals(256,BuilderSkill.BRUSH_AREA.nominal(e.withActive(false)));assertEquals(0,BuilderSkill.BRUSH_AREA.value(e.withActive(false)));
        config.maxAreaBlocks=17;
        assertEquals(17,BuilderSkill.AREA.value(e));assertEquals(17,BuilderSkill.BRUSH_AREA.value(e));
    }
    @Test void initialSelectionIsAtomicAndOnlyCompletesFreeSlots(){
        var p=new PlayerProfile();
        assertFalse(InitialSelection.apply(p,List.of(SkillType.MINER,SkillType.MINER),false).isEmpty());assertTrue(p.selected.isEmpty());
        assertEquals("",InitialSelection.apply(p,List.of(SkillType.MINER,SkillType.WARRIOR),false));
        p.stats(SkillType.MINER).highestLevel=75;p.stats(SkillType.MINER).prestige=1;p.seals=5;
        assertFalse(InitialSelection.apply(p,List.of(SkillType.FISHER),false).isEmpty());assertEquals(2,p.selected.size());
        assertEquals("",InitialSelection.apply(p,List.of(SkillType.MINER,SkillType.WARRIOR,SkillType.BUILDER,SkillType.EXPLORER),false));
        assertEquals(5,p.seals);assertEquals(75,p.stats(SkillType.MINER).highestLevel);assertEquals(1,p.stats(SkillType.MINER).prestige);
        assertFalse(InitialSelection.apply(p,List.of(SkillType.MINER,SkillType.WARRIOR,SkillType.BUILDER,SkillType.EXPLORER,SkillType.FARMER),false).isEmpty());
        assertEquals(4,p.selected.size());
    }
    @Test void gameplayCapsRetainTheExactFloatResultsOf21(){
        float base=7.25f,modified=23.9f;
        assertEquals(Math.min(modified,base*(float)1.4),PowerComposition.capDamage(base,modified,false,.4));
        assertEquals(Math.min(modified,base*1.75f),PowerComposition.capDamage(base,modified,true,.4));
        assertEquals(Math.max(1.4f,base*(float).75),PowerComposition.capResistance(base,1.4f,.25));
    }
    @Test void completeRadiusEightIncludesTheOuterHorizontalShell(){
        var iterator=new ShellPositions(BlockPos.ZERO,8);Set<BlockPos> positions=new HashSet<>();iterator.forEachRemaining(positions::add);
        assertEquals(4913,positions.size());assertTrue(positions.contains(new BlockPos(8,0,0)));assertTrue(positions.contains(new BlockPos(-8,8,-8)));
    }
    @Test void professionPowerGoldenValuesFrom21ArePreserved(){
        // Each row: inactive A/B, active A/B at level100, prestige0, one selected skill.
        AbstractSkill[] skills={new MinerSkill(),new WarriorSkill(),new ArcherSkill(),new FarmerSkill(),new FisherSkill(),new BlacksmithSkill(),new BuilderSkill(),new ExplorerSkill()};
        String[] ids={"miner.speed","warrior.damage","archer.damage","farmer.yield","fisher.wait","smith.skin","reach","speed"};
        double[][] expected={{.5,.8,1.75,2.05},{.28,.4,.88,1},{.25,.25,.75,.75},{.25,.49,.25,.49},{.59,.35,.75,.55},{.08,.14,.18,.24},{3,2.25,3,2.25},{.2,.15,.2,.15}};
        for(int i=0;i<skills.length;i++){
            var type=skills[i].type();final String id=ids[i];var def=skills[i].effects().stream().filter(d->d.id().equals(id)).findFirst().orElseThrow();
            for(int active=0;active<2;active++)for(int branch=1;branch<=2;branch++){
                var p=new PlayerProfile();p.selected.add(type);var stats=p.stats(type);stats.level=100;stats.highestLevel=100;stats.branch=branch;
                var e=SkillEvaluation.of(p,type,new ModConfig(),active==1,false,false,false,false);
                assertEquals(expected[i][active*2+branch-1],def.nominal(e),.000001,type+" branch"+branch+" active"+active);
            }
        }
    }
    @Test void abilityDurationsCooldownsAndAreaCostsAreUnchanged(){
        int[] levels={20,40,60,80,100},seconds={11,14,17,20,27},cooldowns={135,120,105,85,65};
        var p=new PlayerProfile();p.selected.add(SkillType.MINER);
        for(int i=0;i<levels.length;i++){
            var stats=p.stats(SkillType.MINER);stats.level=levels[i];stats.highestLevel=levels[i];
            var e=SkillEvaluation.of(p,SkillType.MINER,new ModConfig(),false,false,false,false,false);
            assertEquals(seconds[i]*20,EffectNumbers.duration(e));assertEquals(cooldowns[i]*20,EffectNumbers.cooldown(e));
        }
        assertEquals(3,ProgressionCurve.areaWeight(9));assertEquals(8,ProgressionCurve.areaWeight(256));assertEquals(153,ProgressionCurve.toolLimit(100,0));
    }
}
