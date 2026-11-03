package com.murilloskills.skills.effects;
import com.murilloskills.skills.SkillType;
import java.util.List;

public record SkillSummary(SkillType skill,boolean selected,int level,int highest,int prestige,String ability,int activeTier,int durationTicks,int cooldownTicks,
        List<EffectView> effects,List<MilestoneView> milestones,List<ControlView> controls,List<BranchPreview> branches,List<String> limits,List<String> activeBenefits) {
    public enum Group { PASSIVE,UTILITY,ACTIVE }
    public enum State { LOCKED,DISABLED,CONDITIONAL,APPLIED }
    public record EffectView(String id,String title,String value,String nextValue,String condition,int unlock,boolean unlocked,boolean enabled,String limit,
        double nominal,double applied,EffectDefinition.Unit unit,Group group,State state){}
    public record MilestoneView(int level,String title,List<String> benefits,boolean unlocked){}
    public record ControlView(String id,String title,String description,int level,boolean available,boolean enabled){}
    public record BranchPreview(int choice,String title,String description,List<Comparison> current,List<TierPreview> tiers){}
    public record Comparison(String title,String before,String after,String condition){}
    public record TierPreview(int requiredLevel,int evaluatedLevel,List<String> improvements,List<Comparison> effects){}
}
