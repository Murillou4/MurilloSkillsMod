package com.murilloskills.skills;
import com.murilloskills.api.SkillContext;
import net.minecraft.resources.Identifier;
public final class AchievementService {
    private AchievementService(){}
    public static void grant(SkillContext c,String id){
        var advancement=c.store().server.getAdvancements().get(Identifier.fromNamespaceAndPath("murilloskills",id));
        if(advancement!=null){var progress=c.player().getAdvancements().getOrStartProgress(advancement);for(var criterion:progress.getRemainingCriteria())c.player().getAdvancements().award(advancement,criterion);}
    }
    public static void reconcile(SkillContext c){
        if(!c.profile().selected.isEmpty())grant(c,"journey");
        for(var t:c.profile().selected){if(c.stats(t).highestLevel>=100)grant(c,"master_"+t.name().toLowerCase(java.util.Locale.ROOT));if(c.stats(t).prestige>0)grant(c,"prestige");}
        if(c.profile().completedSets>0)grant(c,"contracts");
        if(c.profile().paragon!=null)grant(c,"paragon");
    }
}

