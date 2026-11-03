package com.murilloskills.validation;

import com.murilloskills.*;
import com.murilloskills.api.*;
import com.murilloskills.data.*;
import com.murilloskills.events.GameplayEvents;
import com.murilloskills.impl.*;
import com.murilloskills.skills.*;
import com.murilloskills.network.*;
import com.murilloskills.utils.*;
import net.minecraft.server.level.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.*;
import net.minecraft.world.effect.*;
import java.util.*;
import java.nio.file.*;
import static com.murilloskills.validation.Checks.check;

/** In-world probes call native placement, destruction, menus, damage and production server handlers. */
public final class GameplayProbes {
    private GameplayProbes(){}
    public static void run(ServerPlayer player)throws Exception{
        AdminCommandProbes.run(player);
        Core22Probes.run(player);
        Core23Probes.run(player);
        Core21Probes.run(player);
        check(SkillRegistry.all().size()==8,"registry: all eight implementations");
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var c=GameplayEvents.context(player);var p=new PlayerProfile();c.store().replace(player,p);
        for(var type:SkillType.values()){
            p.selected.clear();p.selected.add(type);
            var stats=p.stats(type);stats.level=100;stats.highestLevel=100;stats.branch=1;stats.claimedMilestone=100;
            c.runtime().activeUntil.clear();stats.cooldownUntil=0;p.globalCooldownUntil=0;p.equipped=type;
            GameplayEvents.refresh(c);var skill=SkillRegistry.get(type);
            check(ProgressionService.activate(p,type,c.now(),skill.cooldown(c)),"active."+type+".accepted");
            skill.activate(c);
            check(c.runtime().active(type,c.now())&&stats.cooldownUntil>c.now(),"active."+type+".state_and_cooldown");
            check(!ProgressionService.activate(p,type,c.now(),skill.cooldown(c)),"active."+type+".cooldown_enforced");
            check(MilestoneCatalog.milestones(type).size()==20,"milestones."+type+".twenty");
            check(stats.branchTier()==3,"branch."+type+".third_tier");
        }
        c.runtime().activeUntil.clear();p.globalCooldownUntil=0;
        p.selected.clear();p.selected.addAll(List.of(SkillType.MINER,SkillType.BUILDER,SkillType.WARRIOR,SkillType.EXPLORER));
        p.tools.size=3;p.tools.depth=1;p.tools.miningShape="3X3";p.tools.placementShape="PLANE";
        p.stats(SkillType.BUILDER).branch=0;p.stats(SkillType.BUILDER).level=20;p.stats(SkillType.BUILDER).highestLevel=20;
        p.stats(SkillType.WARRIOR).level=100;p.stats(SkillType.WARRIOR).highestLevel=100;
        p.stats(SkillType.EXPLORER).level=100;p.stats(SkillType.EXPLORER).highestLevel=100;
        GameplayEvents.refresh(c);
        check(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_BREAK_SPEED)>1,"passive.MINER.native_break_speed");
        check(player.getMaxHealth()>20,"passive.WARRIOR.native_health");
        check(player.blockInteractionRange()>4.5,"passive.BUILDER.native_reach");
        check(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)>0.1,"passive.EXPLORER.native_speed");
        p.featureSettings.put("step_assist",true);GameplayEvents.refresh(c);
        check(player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT).hasModifier(MurilloSkills.id("explorer/step")),"toggle.EXPLORER.step_assist_applied");
        p.featureSettings.put("step_assist",false);GameplayEvents.refresh(c);
        check(!player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT).hasModifier(MurilloSkills.id("explorer/step")),"toggle.EXPLORER.step_assist_removed");

        BlockPos origin=player.blockPosition().offset(3,2,0);var world=player.level();
        p.stats(SkillType.MINER).level=20;p.stats(SkillType.MINER).highestLevel=20;
        for(var pos:BlockPos.betweenClosed(origin.offset(-4,-1,-4),origin.offset(8,5,8)))if(world.hasChunkAt(pos)&&world.getBlockEntity(pos)==null)world.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        world.setBlock(origin,Blocks.IRON_ORE.defaultBlockState(),3);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
        c.runtime().nextToolTick=0;
        int before=player.getMainHandItem().getDamageValue();
        check(AreaTools.mine(c,List.of(origin))==1,"gameplay.MINER.native_destruction");
        check(world.getBlockState(origin).isAir()&&player.getMainHandItem().getDamageValue()>before,"gameplay.MINER.tool_wear");
        check(player.getInventory().getNonEquipmentItems().stream().anyMatch(s->s.is(Items.RAW_IRON)),"gameplay.MINER.real_drops_to_inventory");
        world.setBlock(origin,Blocks.IRON_ORE.defaultBlockState(),3);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));c.runtime().nextToolTick=0;
        check(AreaTools.mine(c,List.of(origin))==0&&!world.getBlockState(origin).isAir(),"gameplay.MINER.correct_tool_required");
        world.setBlock(origin,Blocks.BEDROCK.defaultBlockState(),3);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));c.runtime().nextToolTick=0;
        check(AreaTools.mine(c,List.of(origin))==0,"gameplay.MINER.unbreakable_protected");

        BlockPos start=origin.offset(0,0,3);world.setBlock(start.below(),Blocks.STONE.defaultBlockState(),3);
        world.setBlock(start,Blocks.AIR.defaultBlockState(),3);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE,32));c.runtime().nextToolTick=0;
        int placed=AreaTools.place(c,List.of(start));
        check(placed==1&&world.getBlockState(start).is(Blocks.STONE),"gameplay.BUILDER.native_placement");
        check(player.getMainHandItem().getCount()==31,"gameplay.BUILDER.inventory_consumed_exactly");
        check(!c.store().natural(player,start),"gameplay.BUILDER.placed_provenance");
        check(AreaTools.undo(c)==1&&world.getBlockState(start).isAir(),"gameplay.BUILDER.undo");
        check(count(player,Items.STONE)==32&&AreaTools.undo(c)==0,"gameplay.BUILDER.undo_refunds_once");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE,32));c.runtime().nextToolTick=0;
        AreaTools.place(c,List.of(start));world.setBlock(start,Blocks.DIRT.defaultBlockState(),3);
        check(AreaTools.undo(c)==0&&world.getBlockState(start).is(Blocks.DIRT),"gameplay.BUILDER.undo_preserves_changed_blocks");

        double inactiveXp=p.stats(SkillType.FISHER).xp;
        p.stats(SkillType.MINER).level=100;p.stats(SkillType.MINER).highestLevel=100;
        SkillExperience.award(c,"fish",10);
        check(p.stats(SkillType.FISHER).xp==inactiveXp,"progression.inactive_no_xp");
        p.seals=10;
        check(ProgressionService.select(p,SkillType.FISHER,SkillType.MINER,false).isEmpty(),"profession.swap_accepted");
        GameplayEvents.refresh(c);
        check(!player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_BREAK_SPEED).hasModifier(MurilloSkills.id("miner/speed")),"profession.inactive_native_attributes_removed");
        check(p.stats(SkillType.MINER).highestLevel==100&&p.seals==7,"profession.swap_keeps_progress_and_charges");
        p.selected.remove(SkillType.FISHER);p.selected.add(SkillType.MINER);GameplayEvents.refresh(c);

        // A potion belonging to vanilla must survive a profession removal.
        var potion=new MobEffectInstance(MobEffects.NIGHT_VISION,1200,0);
        player.addEffect(potion);p.featureSettings.put("night_vision",true);SkillRegistry.get(SkillType.MINER).tick(c);
        SkillRegistry.get(SkillType.MINER).clear(c);
        check(player.hasEffect(MobEffects.NIGHT_VISION)&&player.getEffect(MobEffects.NIGHT_VISION).getDuration()>1000,"effects.preserve_vanilla_potion");
        player.removeEffect(MobEffects.NIGHT_VISION);
        SkillRegistry.get(SkillType.MINER).tick(c);check(player.hasEffect(MobEffects.NIGHT_VISION),"toggle.MINER.night_vision_applied");
        p.selected.remove(SkillType.MINER);GameplayEvents.refresh(c);
        check(!player.hasEffect(MobEffects.NIGHT_VISION),"effects.inactive_owned_effect_removed");p.selected.add(SkillType.MINER);

        var fisher=(FisherSkill)SkillRegistry.get(SkillType.FISHER);
        p.selected.clear();p.selected.add(SkillType.FISHER);p.stats(SkillType.FISHER).level=100;p.stats(SkillType.FISHER).highestLevel=100;p.stats(SkillType.FISHER).branch=2;
        check(fisher.waitReduction(c)>0&&fisher.luck(c)>=3,"passive.FISHER.wait_and_luck");
        var fish=new ArrayList<ItemStack>();fish.add(new ItemStack(Items.COD));double xp=p.stats(SkillType.FISHER).xp;p.stats(SkillType.FISHER).level=99;
        fisher.caught(c,fish);check(p.stats(SkillType.FISHER).xp>xp,"gameplay.FISHER.catch_reward");
        player.removeEffect(MobEffects.LUCK);fisher.tick(c);check(player.hasEffect(MobEffects.LUCK),"effects.FISHER.luck_applied");
        p.selected.remove(SkillType.FISHER);GameplayEvents.refresh(c);check(!player.hasEffect(MobEffects.LUCK),"effects.FISHER.inactive_luck_removed");
        var farmer=(FarmerSkill)SkillRegistry.get(SkillType.FARMER);
        p.selected.clear();p.selected.add(SkillType.FARMER);p.stats(SkillType.FARMER).level=50;p.stats(SkillType.FARMER).highestLevel=50;
        BlockPos crop=start.offset(2,0,0);world.setBlock(crop.below(),Blocks.FARMLAND.defaultBlockState(),3);world.setBlock(crop,((CropBlock)Blocks.WHEAT).getStateForAge(7),3);
        xp=p.stats(SkillType.FARMER).xp;GameplayEvents.broken(player,crop,world.getBlockState(crop));
        check(p.stats(SkillType.FARMER).xp>xp,"gameplay.FARMER.mature_crop_xp");
        xp=p.stats(SkillType.FARMER).xp;GameplayEvents.broken(player,crop,((CropBlock)Blocks.WHEAT).getStateForAge(0));
        check(p.stats(SkillType.FARMER).xp==xp,"gameplay.FARMER.immature_crop_no_xp");
        world.setBlock(crop,((CropBlock)Blocks.WHEAT).getStateForAge(0),3);farmer.activate(c);
        check(((CropBlock)Blocks.WHEAT).getAge(world.getBlockState(crop))>0,"active.FARMER.real_growth");

        p.selected.clear();p.selected.add(SkillType.BLACKSMITH);p.stats(SkillType.BLACKSMITH).level=100;p.stats(SkillType.BLACKSMITH).highestLevel=100;p.stats(SkillType.BLACKSMITH).branch=1;
        var smith=(BlacksmithSkill)SkillRegistry.get(SkillType.BLACKSMITH);
        check(smith.discount(c)>0&&smith.furnaceMultiplier(c)>1&&smith.enchantLimit(c)==1,"passive.BLACKSMITH.discounts_furnace_overenchant");
        var sword=new ItemStack(Items.DIAMOND_SWORD);sword.setDamageValue(30);player.setItemInHand(InteractionHand.MAIN_HAND,sword);
        // Repair callback is tick-based; run it at a true native boundary in the separate timed probe.
        check(smith.incoming(c,player.damageSources().onFire(),10)<10,"passive.BLACKSMITH.fire_damage_reduction");

        p.selected.clear();p.selected.add(SkillType.ARCHER);p.stats(SkillType.ARCHER).level=100;p.stats(SkillType.ARCHER).highestLevel=100;p.stats(SkillType.ARCHER).branch=1;
        var archer=(ArcherSkill)SkillRegistry.get(SkillType.ARCHER);
        check(archer.arrowSpeed(c)>1&&archer.armorPenetration(c)>0,"passive.ARCHER.arrow_speed_and_penetration");
        p.selected.clear();p.selected.add(SkillType.WARRIOR);
        check(SkillRegistry.get(SkillType.WARRIOR).outgoing(c,player,player.damageSources().playerAttack(player),10)>10,"passive.WARRIOR.melee_damage");
        p.selected.clear();p.selected.add(SkillType.EXPLORER);
        check(((ExplorerSkill)SkillRegistry.get(SkillType.EXPLORER)).hungerMultiplier(c)<1,"passive.EXPLORER.hunger");
        check(SkillRegistry.get(SkillType.EXPLORER).incoming(c,player.damageSources().fall(),10)<10,"passive.EXPLORER.fall_reduction");
        p.selected.clear();p.selected.addAll(List.of(SkillType.MINER,SkillType.BUILDER,SkillType.WARRIOR,SkillType.EXPLORER));
        check(SynergyService.active(p).size()>0,"synergies.selected_pairs");
        for(var type:java.util.List.of(SkillType.ARCHER,SkillType.WARRIOR,SkillType.BUILDER)){
            c.runtime().activeUntil.put(type,c.now()+200);c.runtime().combos.put(type,new ActionCombo());
            SkillRegistry.get(type).clear(c);
            check(!c.runtime().activeUntil.containsKey(type)&&!c.runtime().combos.containsKey(type),"profession.cleanup."+type);
        }
        for(var e:SynergyService.Effect.values())check(SynergyService.bonus(p,e)<=0.1,"synergies.cap."+e);

        // Invalid packet sessions cannot spend seals or change branches.
        var run=c.runtime();run.packetCount=0;long seq=run.lastRequest+1;int seals=p.seals;int branch=p.stats(SkillType.MINER).branch;
        Wire.handle(player,new Protocol.Action(run.session+1,seq,Protocol.Op.BRANCH,"MINER",2));
        check(p.seals==seals&&p.stats(SkillType.MINER).branch==branch,"network.reject_foreign_session");
        run.combatUntil=c.now()+200;run.packetCount=0;
        Wire.handle(player,new Protocol.Action(run.session,seq+1,Protocol.Op.BRANCH,"MINER",2));
        check(p.seals==seals&&p.stats(SkillType.MINER).branch==branch,"network.combat_cannot_spend_seals");
        run.combatUntil=0;run.activeUntil.clear();
        run.activeUntil.put(SkillType.MINER,c.now()+600);p.equipped=SkillType.WARRIOR;p.globalCooldownUntil=0;p.stats(SkillType.WARRIOR).cooldownUntil=0;run.packetCount=0;
        Wire.handle(player,new Protocol.Action(run.session,seq+2,Protocol.Op.ACTIVATE,"",0));
        check(run.active(SkillType.MINER,c.now())&&run.active(SkillType.WARRIOR,c.now()),"active.independent_cooldowns_after_global_interval");
        run.activeUntil.clear();
        for(var t:SkillType.values()){var s=p.stats(t);s.level=0;s.highestLevel=0;s.xp=0;s.cooldownUntil=0;s.branch=0;}
        int[] showcase={100,34,0,0,0,0,58,22};
        for(var t:p.selected){var s=p.stats(t);s.level=showcase[t.ordinal()];s.highestLevel=s.level;s.xp=s.level==100?0:ProgressionCurve.required(t,s.level)*0.43;s.claimedMilestone=s.level/5*5;}
        p.globalCooldownUntil=0;p.seals=12;p.equipped=SkillType.MINER;p.paragon=null;p.hasChosenParagon=false;p.featureSettings.put("night_vision",false);
        ContractService.ensure(p);p.contracts.getFirst().progress=3;c.store().save(player);
        var reloaded=ProfileCodec.read(c.store().path(player.getUUID()));
        check(reloaded.contracts.getFirst().progress==3&&reloaded.selected.size()==4,"persistence.real_world_file");
        c.runtime().lastRequest=0;c.runtime().packetCount=0;
        GameplayEvents.refresh(c);Wire.send(player,"");
    }
    private static int count(ServerPlayer p,Item item){return p.getInventory().getNonEquipmentItems().stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();}
}

