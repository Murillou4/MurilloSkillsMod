package com.murilloskills;
import com.murilloskills.data.*;
import com.murilloskills.skills.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class ProgressionTest {
    private PlayerProfile selected(SkillType... skills){var p=new PlayerProfile();p.selected.addAll(List.of(skills));return p;}
    private void max(PlayerProfile p,SkillType t){ProgressionService.addXp(p,t,1e9,1);}
    @Test void onlyFourSelectedAndInactiveRetainsProgress(){
        var p=selected(SkillType.MINER,SkillType.WARRIOR,SkillType.FARMER,SkillType.BUILDER);
        assertFalse(ProgressionService.select(p,SkillType.FISHER,null,false).isEmpty());
        p.stats(SkillType.MINER).level=15;p.stats(SkillType.MINER).highestLevel=15;p.seals=3;
        assertEquals("",ProgressionService.select(p,SkillType.FISHER,SkillType.MINER,false));
        assertEquals(0,ProgressionService.addXp(p,SkillType.MINER,1000,1).amount());
        assertEquals(15,p.stats(SkillType.MINER).level);assertEquals(0,p.seals);
    }
    @Test void allFourReach100WithoutParagon(){
        var p=selected(SkillType.MINER,SkillType.WARRIOR,SkillType.FARMER,SkillType.BUILDER);
        for(var t:p.selected){max(p,t);assertEquals(100,p.stats(t).level);}
        assertNull(p.paragon);
    }
    @Test void largeGrantRewardsEveryCrossedMilestoneOnlyOnce(){
        var p=selected(SkillType.MINER);var gain=ProgressionService.addXp(p,SkillType.MINER,1e9,1);
        assertEquals(20,gain.firstMilestones().size());assertEquals(0,ProgressionService.addXp(p,SkillType.MINER,100,1).firstMilestones().size());
    }
    @Test void prestigeKeepsUnlocksBranchAndClaimsButResetsNumericalLevel(){
        var p=selected(SkillType.MINER);max(p,SkillType.MINER);p.stats(SkillType.MINER).branch=2;
        assertEquals("",ProgressionService.prestige(p,SkillType.MINER,false));
        var s=p.stats(SkillType.MINER);assertEquals(0,s.level);assertEquals(100,s.unlockLevel());assertEquals(5,s.activeTier());assertEquals(2,s.branch);
        assertEquals(0,ProgressionService.addXp(p,SkillType.MINER,1e9,1).firstMilestones().size());
        assertEquals(20,p.achievements.size());
    }
    @Test void rejectsNonfiniteAndNegativeXp(){var p=selected(SkillType.MINER);for(double n:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1,0})assertEquals(0,ProgressionService.addXp(p,SkillType.MINER,n,1).amount());assertEquals(0,p.stats(SkillType.MINER).level);}
    @Test void branchAndParagonCannotSpendDuringCombat(){
        var p=selected(SkillType.MINER);max(p,SkillType.MINER);p.seals=10;
        assertFalse(ProgressionService.branch(p,SkillType.MINER,1,true).isEmpty());assertEquals(10,p.seals);
        assertEquals("",ProgressionService.branch(p,SkillType.MINER,1,false));assertEquals(10,p.seals);
        assertEquals("",ProgressionService.branch(p,SkillType.MINER,2,false));assertEquals(9,p.seals);
        assertEquals("",ProgressionService.paragon(p,SkillType.MINER,false));assertEquals(9,p.seals);
    }
    @Test void cooldownsPersistAndCannotBeBypassedByEquippingAnotherSkill(){
        var p=selected(SkillType.MINER,SkillType.WARRIOR);max(p,SkillType.MINER);max(p,SkillType.WARRIOR);
        assertTrue(ProgressionService.activate(p,SkillType.MINER,1000,1000));
        assertFalse(ProgressionService.activate(p,SkillType.WARRIOR,1099,1000));
        assertTrue(ProgressionService.activate(p,SkillType.WARRIOR,1100,1000));
        assertFalse(ProgressionService.activate(p,SkillType.MINER,1500,1000));
    }
    @Test void completeThreeContractsAwardsExactlyOneSeal(){
        var p=selected(SkillType.MINER);ContractService.ensure(p);
        ContractService.progress(p,SkillType.MINER,"mine",180,1);
        assertEquals(1,p.seals);assertEquals(1,p.completedSets);assertEquals(3,p.contracts.size());
        assertTrue(p.contracts.stream().allMatch(c->c.progress==0));
    }
    @Test void areaXpGrowsButCannotScaleLinearlyToThousands(){
        assertEquals(1,ProgressionCurve.areaWeight(1));assertEquals(8,ProgressionCurve.areaWeight(1000));
        assertTrue(ProgressionCurve.areaWeight(8)>1);assertEquals(0,ProgressionCurve.areaWeight(0));
    }
    @Test void catalogHasTwentyDistinctMilestonesForEachProfession(){
        for(var t:SkillType.values()){
            var entries=MilestoneCatalog.milestones(t);assertEquals(20,entries.size());
            for(int i=0;i<20;i++)assertEquals((i+1)*5,entries.get(i).level());
        }
    }
}

