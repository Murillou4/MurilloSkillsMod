package com.murilloskills.skills.effects;
import com.murilloskills.data.*;
import com.murilloskills.config.ModConfig;
import com.murilloskills.skills.*;
import java.util.*;

/** Immutable evaluation inputs; projections never mutate player progression or Minecraft. */
public record SkillEvaluation(PlayerProfile profile,SkillType type,ModConfig config,int level,int highest,int branch,int branchTier,
        boolean selected,boolean active,boolean paragon,boolean water,boolean lava,boolean drawing,boolean combat) {
    public static SkillEvaluation of(PlayerProfile p,SkillType type,ModConfig config,boolean active,boolean water,boolean lava,boolean drawing,boolean combat){
        var s=p.stats(type);return new SkillEvaluation(p,type,config,s.level,s.unlockLevel(),s.branch,s.branchTier(),p.selected(type),active,p.paragon==type,water,lava,drawing,combat);
    }
    public double scale(){return level/100d*ProgressionCurve.passiveBonus(profile.stats(type).prestige);}
    public SkillEvaluation withActive(boolean value){return new SkillEvaluation(profile,type,config,level,highest,branch,branchTier,selected,value,paragon,water,lava,drawing,combat);}
    public SkillEvaluation atMilestone(int value,int choice){
        int rank=choice==0?0:value>=75?3:value>=50?2:value>=25?1:0;
        return new SkillEvaluation(profile,type,config,value,value,choice,rank,true,false,paragon,water,lava,drawing,combat);
    }
    public int rank(int choice){return branch==choice?branchTier:0;}
    public boolean has(int required){return selected&&highest>=required;}
    public int tier(){return highest/20;}
    public boolean feature(Feature feature){return feature.enabled(profile);}
    public double synergy(SynergyService.Effect effect){return SynergyService.bonus(profile,effect);}
    public SkillEvaluation preview(int atLevel,int choice){
        int h=Math.max(atLevel,highest),tier=choice==0?0:h>=75?3:h>=50?2:h>=25?1:0;
        return new SkillEvaluation(profile,type,config,atLevel,h,choice,tier,true,false,paragon,water,lava,drawing,combat);
    }
    public SkillEvaluation branchPreview(int choice,int rank){
        int required=rank==3?75:rank==2?50:25;
        int projectedLevel=highest>=required?level:Math.max(level,required);
        return new SkillEvaluation(profile,type,config,projectedLevel,Math.max(highest,required),choice,rank,true,false,paragon,water,lava,drawing,combat);
    }
}
