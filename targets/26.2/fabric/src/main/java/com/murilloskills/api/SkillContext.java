package com.murilloskills.api;
import com.murilloskills.*;
import com.murilloskills.data.*;
import com.murilloskills.skills.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public record SkillContext(ServerPlayer player,PlayerStore store) {
    public SkillContext {
        var runtime=store.runtime(player);String current=player.level().dimension().identifier().toString();
        if(!current.equals(runtime.dimension)){
            runtime.dimension=current;runtime.pendingCollections.clear();runtime.scans.clear();
            runtime.pendingMiningAnchor=null;runtime.pendingMiningPlan=java.util.List.of();runtime.queuedMiningPlan=java.util.List.of();runtime.lastPosition=null;
            runtime.brushFirst=null;runtime.brushSecond=null;runtime.brushDimension="";
            runtime.growth.clear();runtime.activities.clear();runtime.impacts.clear();runtime.lastRadarOre=null;runtime.lastRadarDistance=Double.MAX_VALUE;
        }
    }
    public PlayerProfile profile(){return store.get(player);}
    public RuntimeState runtime(){return store.runtime(player);}
    public long now(){return store.now();}
    public SkillStats stats(SkillType type){return profile().stats(type);}
    public com.murilloskills.skills.effects.SkillEvaluation evaluate(SkillType type){return com.murilloskills.skills.effects.SkillEvaluation.of(profile(),type,store.config,active(type),player.isInWater(),player.isInLava(),player.isUsingItem()&&(player.getUseItem().is(net.minecraft.world.item.Items.BOW)||player.getUseItem().is(net.minecraft.world.item.Items.CROSSBOW)),runtime().combatUntil>now());}
    public boolean feature(com.murilloskills.skills.effects.Feature feature){return feature.available(profile())&&feature.enabled(profile());}
    public boolean has(SkillType type,int unlock){return profile().unlocked(type,unlock);}
    public boolean perk(SkillPerk perk){return has(perk.skill,perk.level);}
    public boolean active(SkillType type){return profile().selected(type)&&runtime().active(type,now());}
    public double scale(SkillType type){return stats(type).level/100d*ProgressionCurve.passiveBonus(stats(type).prestige);}
    public int branch(SkillType type,int choice){return profile().selected(type)&&stats(type).branch==choice?stats(type).branchTier():0;}
    public boolean paragon(SkillType type){return profile().paragon==type&&has(type,100);}
    public void xp(String action,double units){SkillExperience.award(this,action,units);}
    public boolean chance(double probability){return player.getRandom().nextDouble()<probability;}
    public void message(String text){player.sendSystemMessage(Component.literal(text));}
    public void give(ItemStack stack){if(stack.isEmpty())return;player.getInventory().add(stack);if(!stack.isEmpty())player.drop(stack,false);}
}

