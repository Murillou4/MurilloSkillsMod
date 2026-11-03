package com.murilloskills.validation;
import com.murilloskills.*;
import com.murilloskills.api.*;
import com.murilloskills.data.*;
import com.murilloskills.events.GameplayEvents;
import com.murilloskills.impl.*;
import com.murilloskills.skills.*;
import com.murilloskills.skills.effects.*;
import com.murilloskills.utils.*;
import com.murilloskills.network.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.phys.AABB;
import java.util.*;
import static com.murilloskills.validation.Checks.check;

/** Focused recovery probes, executed once with the overall native review. */
public final class Core21Probes {
    private Core21Probes(){}
    public static void run(ServerPlayer player){
        var c=GameplayEvents.context(player);var p=new PlayerProfile();c.store().replace(player,p);
        p.selected.addAll(List.of(SkillType.FARMER,SkillType.BUILDER,SkillType.ARCHER,SkillType.EXPLORER));
        for(var t:p.selected){p.stats(t).level=100;p.stats(t).highestLevel=100;}
        c.runtime().activeUntil.clear();c.runtime().nextToolTick=0;c.runtime().combatUntil=0;
        for(var skill:SkillRegistry.all()){
            check(!skill.effects().isEmpty(),"v21.catalog."+skill.type()+".has_numeric_definitions");
            var view=SummaryService.build(c,skill.type());check(view.milestones().size()==20&&view.branches().size()==2,"v21.catalog."+skill.type()+".complete_server_summary");
        }
        p.featureSettings.put(Feature.BONEMEAL.id,true);
        var center=player.blockPosition().offset(3,0,3);var crop=(CropBlock)Blocks.WHEAT;
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var at=center.offset(x,0,z);player.level().setBlock(at.below(),Blocks.FARMLAND.defaultBlockState(),3);player.level().setBlock(at,crop.getStateForAge(0),3);}
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BONE_MEAL,64));
        int used=FarmingTools.boneMeal(c,center);
        check(used>=9&&player.getMainHandItem().getCount()==64-used,"v21.farmer.bonemeal_consumes_per_native_use");
        check(crop.getAge(player.level().getBlockState(center))>0,"v21.farmer.bonemeal_grows_crops");
        var composter=center.offset(3,0,0);var full=Blocks.COMPOSTER.defaultBlockState().setValue(ComposterBlock.LEVEL,8);player.level().setBlock(composter,full,3);
        int before=loose(player,Items.BONE_MEAL);ComposterBlock.extractProduce(player,full,player.level(),composter);
        check(loose(player,Items.BONE_MEAL)-before==2,"v21.farmer.composter_real_two_outputs");
        p.stats(SkillType.BUILDER).level=20;p.stats(SkillType.BUILDER).highestLevel=20;
        p.equipped=SkillType.BUILDER;SkillRegistry.get(SkillType.BUILDER).activate(c);
        var a=center.offset(0,2,0);var b=a.offset(2,0,1);
        for(var at:BlockPos.betweenClosed(a.below(),b.below()))player.level().setBlock(at,Blocks.STONE.defaultBlockState(),3);
        for(var at:BlockPos.betweenClosed(a,b))player.level().setBlock(at,Blocks.AIR.defaultBlockState(),3);
        c.runtime().brushFirst=a;c.runtime().brushSecond=b;c.runtime().brushDimension=player.level().dimension().identifier().toString();
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE,5));c.runtime().nextToolTick=0;
        check(BrushTools.plan(c).size()==6,"v21.builder.two_corner_preview_six");
        BrushTools.execute(c);check(player.getMainHandItem().getCount()==5&&player.level().getBlockState(a).isAir(),"v21.builder.insufficient_materials_atomic_rejection");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE,12));int planned=BrushTools.plan(c).size();String result=BrushTools.execute(c);
        check(planned==6&&player.getMainHandItem().getCount()==6&&player.level().getBlockState(b).is(Blocks.STONE),"v21.builder.brush_real_placement_consumption");
        SkillRegistry.get(SkillType.BUILDER).cancel(c);
        check(AreaTools.undo(c)==6,"v21.builder.brush_native_undo");

        var archer=(ArcherSkill)SkillRegistry.get(SkillType.ARCHER);
        var arrow=new Arrow(player.level(),player,new ItemStack(Items.ARROW),new ItemStack(Items.BOW));
        arrow.setPos(player.getX(),player.getY()+1,player.getZ());arrow.setDeltaMovement(1,0,0);arrow.tick();
        check(arrow.getPierceLevel()==2,"v21.archer.native_piercing_level");
        p.paragon=SkillType.ARCHER;archer.activate(c);
        var hostile=new Zombie(player.level());hostile.setNoAi(true);hostile.setPos(arrow.getX()+7,arrow.getY(),arrow.getZ()+2);player.level().addFreshEntity(hostile);
        arrow.tickCount=5;var previous=arrow.getDeltaMovement();archer.guide(c,arrow);
        check(arrow.getDeltaMovement().z>previous.z,"v21.archer.paragon_guidance_changes_trajectory");
        archer.cancel(c);var velocity=arrow.getDeltaMovement();archer.guide(c,arrow);
        check(arrow.getDeltaMovement().equals(velocity),"v21.archer.cancel_stops_guidance");
        arrow.discard();hostile.discard();

        p.featureSettings.put(Feature.NETHER.id,true);
        check(GameplayEvents.damage(player,player.damageSources().hotFloor(),4)==0,"v21.explorer.magma_assistance");
        p.featureSettings.put(Feature.NETHER.id,false);
        check(GameplayEvents.damage(player,player.damageSources().hotFloor(),4)>0,"v21.explorer.magma_toggle_off");

        p.selected.clear();p.selected.addAll(List.of(SkillType.BLACKSMITH,SkillType.WARRIOR));
        for(var t:p.selected){p.stats(t).level=99;p.stats(t).highestLevel=100;}
        p.featureSettings.put(Feature.REFLECT.id,true);p.paragon=null;GameplayEvents.refresh(c);
        var attacker=new Zombie(player.level());attacker.setNoAi(true);attacker.setPos(player.getX()+2,player.getY(),player.getZ());player.level().addFreshEntity(attacker);
        float health=attacker.getHealth();player.setHealth(player.getMaxHealth()-4);float playerHealth=player.getHealth();
        double warriorXp=p.stats(SkillType.WARRIOR).xp,smithXp=p.stats(SkillType.BLACKSMITH).xp;
        var smith=(BlacksmithSkill)SkillRegistry.get(SkillType.BLACKSMITH);
        for(int i=0;i<200&&attacker.getHealth()==health;i++)smith.reflect(c,player.damageSources().mobAttack(attacker),8);
        check(attacker.getHealth()<health,"v21.smith.reflection_deals_native_damage");
        check(player.getHealth()==playerHealth&&p.stats(SkillType.WARRIOR).xp==warriorXp&&p.stats(SkillType.BLACKSMITH).xp==smithXp,"v21.smith.reflection_no_leech_or_xp");
        float reflectedHealth=attacker.getHealth();c.runtime().reflecting=true;smith.reflect(c,player.damageSources().mobAttack(attacker),8);c.runtime().reflecting=false;
        check(attacker.getHealth()==reflectedHealth,"v21.smith.reflection_chain_guard");attacker.discard();player.setHealth(player.getMaxHealth());

        p.selected.clear();p.selected.add(SkillType.MINER);p.stats(SkillType.MINER).level=100;p.stats(SkillType.MINER).highestLevel=100;p.equipped=SkillType.MINER;p.paragon=null;
        c.runtime().activeUntil.clear();p.stats(SkillType.MINER).cooldownUntil=0;p.globalCooldownUntil=0;c.runtime().packetCount=0;
        var run=c.runtime();long request=run.lastRequest+1;
        Wire.handle(player,new Protocol.Action(run.session,request++,Protocol.Op.SET_FEATURE,Feature.NIGHT_VISION.id,1));
        SkillRegistry.get(SkillType.MINER).tick(c);check(player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),"v21.controls.feature_enabled");
        Wire.handle(player,new Protocol.Action(run.session,request++,Protocol.Op.SET_FEATURE,Feature.NIGHT_VISION.id,0));
        check(!player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),"v21.controls.feature_off_immediate");
        Wire.handle(player,new Protocol.Action(run.session,request++,Protocol.Op.ACTIVATE,"",0));long cooldown=p.stats(SkillType.MINER).cooldownUntil;
        Wire.handle(player,new Protocol.Action(run.session,request++,Protocol.Op.CANCEL_ACTIVE,"",0));
        check(!c.active(SkillType.MINER)&&p.stats(SkillType.MINER).cooldownUntil==cooldown,"v21.active.cancel_keeps_cooldown");
        c.runtime().nextToolTick=0;c.runtime().activeUntil.clear();p.globalCooldownUntil=0;
    }
    private static int loose(ServerPlayer p,Item item){return p.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(p.blockPosition()).inflate(16)).stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();}
}
