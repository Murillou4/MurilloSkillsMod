package com.murilloskills.skills;
import com.murilloskills.data.*;
import java.util.*;

public final class ContractService {
    public static final int SEALS_PER_SET=1;
    private ContractService(){}
    public static void reassign(PlayerProfile p,SkillType outgoing,SkillType incoming){
        var template=new PlayerProfile();template.selected.add(incoming);ensure(template);
        for(var c:p.contracts)if(c.skill==outgoing){var replacement=template.contracts.getFirst();c.skill=incoming;c.activity=replacement.activity;c.target=replacement.target;c.progress=0;c.rewarded=false;}
    }
    public static void ensure(PlayerProfile p) {
        if(!p.contracts.isEmpty()||p.selected.isEmpty())return;
        for(int i=0;i<3;i++){
            SkillType skill=p.selected.get((p.completedSets+i)%p.selected.size());
            String activity=switch(skill){case MINER->"mine";case WARRIOR,ARCHER->"combat";case FARMER->"harvest";case FISHER->"fish";case BLACKSMITH->"forge";case BUILDER->"build";case EXPLORER->"explore";};
            double baseline=switch(skill){case MINER->180;case WARRIOR->30;case ARCHER->20;case FARMER->100;case FISHER->10;case BLACKSMITH->80;case BUILDER->180;case EXPLORER->250;};
            p.contracts.add(new Contract(skill,activity,Math.ceil(baseline*(1+p.stats(skill).level/100.0))));
        }
    }
    public static boolean progress(PlayerProfile p,SkillType skill,String activity,double count,double xpMultiplier) {
        ensure(p);boolean changed=false;
        for(var c:p.contracts)if(c.skill==skill&&c.activity.equals(activity)&&!c.complete()){
            c.progress=Math.min(c.target,c.progress+Math.max(0,count));changed=true;
            if(c.complete()&&!c.rewarded){
                c.rewarded=true;
                // Seals are the main reward. XP is a small supplement, never the dominant leveling loop.
                ProgressionService.addXp(p,skill,ProgressionCurve.required(skill,Math.min(99,p.stats(skill).level))*0.01,xpMultiplier);
            }
        }
        if(!p.contracts.isEmpty()&&p.contracts.stream().allMatch(Contract::complete)){
            p.seals+=SEALS_PER_SET;p.completedSets++;p.achievements.add("contracts:"+p.completedSets);p.contracts.clear();ensure(p);changed=true;
        }
        return changed;
    }
}

