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
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import java.util.*;
import static com.murilloskills.validation.Checks.check;

final class Core22Probes {
    private Core22Probes(){}
    static void run(ServerPlayer player)throws Exception{
        var c=GameplayEvents.context(player);var p=new PlayerProfile();c.store().replace(player,p);p.selected.add(SkillType.FARMER);
        p.stats(SkillType.FARMER).level=100;p.stats(SkillType.FARMER).highestLevel=100;p.equipped=SkillType.FARMER;
        c.runtime().growth.clear();c.runtime().activeUntil.clear();c.runtime().combatUntil=0;
        var crop=(CropBlock)Blocks.WHEAT;var corner=player.blockPosition().offset(8,0,0);
        for(var pos:WorldTools.loadedBox(c,player.blockPosition(),8,4913)){var state=player.level().getBlockState(pos);if(state.getBlock() instanceof CropBlock other)player.level().setBlock(pos,other.getStateForAge(other.getMaxAge()),3);}
        player.level().setBlock(corner.below(),Blocks.FARMLAND.defaultBlockState(),3);player.level().setBlock(corner,crop.getStateForAge(0),3);
        SkillRegistry.get(SkillType.FARMER).activate(c);for(int i=0;i<6;i++)GrowthService.tick(c);
        check(crop.getAge(player.level().getBlockState(corner))==2,"v22.farmer.full_advertised_outer_radius");
        SkillRegistry.get(SkillType.FARMER).cancel(c);check(!c.runtime().growth.containsKey("surge"),"v22.farmer.cancel_clears_jobs");

        p.selected.clear();p.selected.add(SkillType.FISHER);var fs=p.stats(SkillType.FISHER);fs.level=100;fs.highestLevel=100;fs.branch=2;
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.FISHING_ROD));var hook=new FishingHook(player,player.level(),2,0);
        hook.setPos(player.getX()+2,player.getY()+1,player.getZ());player.level().addFreshEntity(hook);
        var luck=FishingHook.class.getDeclaredField("luck");luck.setAccessible(true);check(luck.getInt(hook)==6,"v22.fisher.vanilla_luck_preserved");
        SkillRegistry.get(SkillType.FISHER).activate(c);hook.tick();check(luck.getInt(hook)==7,"v22.fisher.active_applies_to_existing_hook");
        SkillRegistry.get(SkillType.FISHER).cancel(c);hook.tick();check(luck.getInt(hook)==6,"v22.fisher.cancel_removes_active_luck");
        p.selected.clear();hook.tick();check(luck.getInt(hook)==2,"v22.fisher.inactive_hook_restores_vanilla_luck");hook.discard();

        p.selected.add(SkillType.ARCHER);p.stats(SkillType.ARCHER).level=100;p.stats(SkillType.ARCHER).highestLevel=100;p.stats(SkillType.ARCHER).branch=1;
        var hitMethod=AbstractArrow.class.getDeclaredMethod("onHitEntity",EntityHitResult.class);hitMethod.setAccessible(true);
        for(boolean head:new boolean[]{false,true}){
            var zombie=new Zombie(player.level());zombie.setNoAi(true);zombie.setPos(player.getX()+3,player.getY(),player.getZ());player.level().addFreshEntity(zombie);
            var arrow=new Arrow(player.level(),player,new ItemStack(Items.ARROW),new ItemStack(Items.BOW));arrow.setDeltaMovement(1,0,0);
            arrow.setPos(zombie.getX(),zombie.getY()+(head?.2:1.8),zombie.getZ());c.runtime().activities.remove("combat:headshot");
            hitMethod.invoke(arrow,new EntityHitResult(zombie,zombie.position().add(0,head?1.8:.3,0)));
            check(c.runtime().activities.containsKey("combat:headshot")==head,"v22.archer.actual_impact_"+(head?"head":"body"));
            check(c.runtime().impacts.isEmpty(),"v22.archer.impact_scope_cleared");arrow.discard();zombie.discard();
        }
        p.selected.clear();p.selected.add(SkillType.BUILDER);p.stats(SkillType.BUILDER).level=20;p.stats(SkillType.BUILDER).highestLevel=20;p.equipped=SkillType.BUILDER;
        var target=player.blockPosition().offset(3,2,3);player.level().setBlock(target.below(),Blocks.STONE.defaultBlockState(),3);player.level().setBlock(target,Blocks.AIR.defaultBlockState(),3);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE,12));c.runtime().nextToolTick=0;c.runtime().combatUntil=0;
        SkillRegistry.get(SkillType.BUILDER).activate(c);
        check(AreaTools.place(c,List.of(target))==1,"v22.builder.place_before_failed_attempt");c.runtime().nextToolTick=0;
        check(AreaTools.place(c,List.of())==0&&c.runtime().undo.size()==1,"v22.builder.noop_keeps_previous_undo");
        check(AreaTools.undo(c)==1&&c.active(SkillType.BUILDER),"v22.builder.undo_during_own_brush");SkillRegistry.get(SkillType.BUILDER).cancel(c);
        c.runtime().activities.clear();c.runtime().combatUntil=0;c.runtime().activeUntil.clear();
    }
}
