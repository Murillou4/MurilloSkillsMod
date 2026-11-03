package com.murilloskills.skills;
import com.murilloskills.api.*;
import com.murilloskills.network.*;
import net.minecraft.network.chat.Component;

public final class SkillExperience {
    private SkillExperience(){}
    public static void award(SkillContext c,String action,double units){
        if(units<=0||!Double.isFinite(units)||!c.player().gameMode.isSurvival())return;
        var reward=ActionRewards.get(action);if(!c.profile().selected(reward.skill()))return;
        if(c.runtime().area){c.runtime().batchRewards.merge(action,units,Double::sum);return;}
        grant(c,action,units,false);
    }
    public static void grant(SkillContext c,String action,double units,boolean area){
        var reward=ActionRewards.get(action);double weighted=area?ProgressionCurve.areaWeight((int)units):units;
        double craftBonus=reward.skill()==SkillType.BLACKSMITH?Math.max(1,com.murilloskills.impl.BlacksmithSkill.XP.value(c,SkillType.BLACKSMITH)):1;
        double combo=c.runtime().combos.computeIfAbsent(reward.skill(),k->new ActionCombo()).record(c.now());
        var gain=ProgressionService.addXp(c.profile(),reward.skill(),reward.xp()*weighted*craftBonus,c.store().config.xpMultiplier*combo);
        int sealsBefore=c.profile().seals;
        ContractService.progress(c.profile(),reward.skill(),reward.contract(),area?weighted:units,c.store().config.xpMultiplier);
        if(c.profile().seals>sealsBefore)c.player().sendOverlayMessage(Component.literal("Contratos concluídos · +"+(c.profile().seals-sealsBefore)+" selo de reespecialização"));
        c.store().dirty(c.player());
        if(gain.to()>gain.from()){
            SkillRegistry.get(reward.skill()).updateAttributes(c);
            c.player().sendOverlayMessage(Component.literal(reward.skill().title+" · nível "+gain.to()+
                (gain.firstMilestones().isEmpty()?"":" · novo marco!")));
        }
        Wire.xp(c.player(),reward.skill(),gain.amount(),combo);
    }
    public static void flushArea(SkillContext c){
        var rewards=new java.util.HashMap<>(c.runtime().batchRewards);c.runtime().batchRewards.clear();c.runtime().area=false;
        for(var e:rewards.entrySet())grant(c,e.getKey(),e.getValue(),true);
    }
}

