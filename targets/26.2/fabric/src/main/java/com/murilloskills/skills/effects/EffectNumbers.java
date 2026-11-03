package com.murilloskills.skills.effects;
import com.murilloskills.skills.*;
public final class EffectNumbers {
    private EffectNumbers(){}
    public static int duration(SkillEvaluation e){return (8+e.tier()*3+MilestoneCatalog.durationBonus(e.highest()))*20;}
    public static int cooldown(SkillEvaluation e){return Math.max(30,150-e.tier()*15-MilestoneCatalog.cooldownReduction(e.highest()))*20;}
    public static int scanRadius(int base,double finds,int paragon){return Math.min(24,base+(int)Math.ceil(base*finds)+paragon);}
    public static int area(SkillEvaluation e,int branch){return Math.min(e.config().maxAreaBlocks,ProgressionCurve.toolLimit(e.highest(),e.rank(branch))*(e.active()?2:1));}
}
