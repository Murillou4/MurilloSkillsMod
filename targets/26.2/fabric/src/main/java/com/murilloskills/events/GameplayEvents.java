package com.murilloskills.events;
import com.murilloskills.*;
import com.murilloskills.api.*;
import com.murilloskills.skills.*;
import com.murilloskills.impl.*;
import com.murilloskills.network.Wire;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.projectile.Projectile;

public final class GameplayEvents {
    private GameplayEvents(){}
    public static SkillContext context(ServerPlayer p){return new SkillContext(p,MurilloSkills.store);}
    public static void register(){
        PlayerBlockBreakEvents.BEFORE.register((world,player,pos,state,be)->{
            if(player instanceof ServerPlayer p&&MurilloSkills.store!=null){
                var c=context(p);var run=c.runtime();
                if(run.miningHeld&&!run.area&&c.has(SkillType.MINER,15)){
                    run.pendingMiningAnchor=pos.immutable();
                    var hit=p.pick(p.blockInteractionRange(),0,false);
                    var face=hit instanceof net.minecraft.world.phys.BlockHitResult block&&block.getBlockPos().equals(pos)?block.getDirection():p.getDirection().getOpposite();
                    run.pendingMiningPlan=AreaTools.candidates(c,false,pos,face);
                }
            }
            return true;
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((victim,source,base,taken,blocked)->{
            if(MurilloSkills.store!=null&&!blocked&&victim instanceof ServerPlayer target)((BlacksmithSkill)SkillRegistry.get(SkillType.BLACKSMITH)).reflect(context(target),source,taken);
            if(MurilloSkills.store!=null&&!blocked&&!source.is(DamageTypes.THORNS)&&victim instanceof net.minecraft.world.entity.monster.Enemy&&source.getEntity() instanceof ServerPlayer p&&!(source.getDirectEntity() instanceof Projectile)){
                ((WarriorSkill)SkillRegistry.get(SkillType.WARRIOR)).leech(context(p),Math.min(taken,victim.getMaxHealth()));
            }
        });
        PlayerBlockBreakEvents.AFTER.register((world,player,pos,state,be)->{if(player instanceof ServerPlayer p)broken(p,pos,state);});
        ServerLivingEntityEvents.AFTER_DEATH.register((victim,source)->{
            if(source.getEntity() instanceof ServerPlayer p&&MurilloSkills.store!=null)for(var skill:SkillRegistry.all())if(context(p).profile().selected(skill.type()))skill.killed(context(p),victim,source);
        });
        UseBlockCallback.EVENT.register((player,world,hand,hit)->{
            if(player instanceof ServerPlayer p&&MurilloSkills.store!=null){
                var c=context(p);
                for(var skill:SkillRegistry.all())if(c.profile().selected(skill.type())&&skill.useBlock(c,hand,hit))return net.minecraft.world.InteractionResult.SUCCESS;
            }
            return net.minecraft.world.InteractionResult.PASS;
        });
    }
    public static void broken(ServerPlayer player,BlockPos pos,BlockState state){
        if(MurilloSkills.store==null)return;
        var c=context(player);boolean natural=c.store().natural(player,pos);
        if(!c.runtime().area&&pos.equals(c.runtime().pendingMiningAnchor)){
            c.runtime().queuedMiningPlan=c.runtime().pendingMiningPlan.stream().filter(p->!p.equals(pos)).toList();
            c.runtime().pendingMiningPlan=java.util.List.of();c.runtime().pendingMiningAnchor=null;
        }
        for(var skill:SkillRegistry.all())if(c.profile().selected(skill.type()))skill.broken(c,pos,state,natural);
        if(c.has(SkillType.MINER,15))c.runtime().pendingCollections.add(pos.immutable());
        c.store().removed(player,pos);
    }
    public static boolean placed(ServerPlayer player,BlockPos pos,BlockState state){
        if(MurilloSkills.store==null)return false;var c=context(player);boolean first=c.store().placed(player,pos);
        for(var skill:SkillRegistry.all())if(c.profile().selected(skill.type()))skill.placed(c,pos,state,first);
        return first;
    }
    public static float damage(LivingEntity victim,DamageSource source,float amount){
        if(MurilloSkills.store==null||amount<=0)return amount;
        float value=amount;
        if(!source.is(DamageTypes.THORNS)&&source.getEntity() instanceof ServerPlayer attacker){
            var c=context(attacker);c.runtime().combatUntil=c.now()+200;
            for(var skill:SkillRegistry.all())if(c.profile().selected(skill.type()))value=skill.outgoing(c,victim,source,value);
            value*=1+SynergyService.bonus(c.profile(),SynergyService.Effect.DAMAGE);
            value=(float)PowerComposition.capDamage(amount,value,c.runtime().activeUntil.values().stream().anyMatch(t->t>c.now()),c.store().config.maxDamageBonus);
            if(source.getDirectEntity() instanceof Projectile arrow&&ArcherSkill.HEADSHOT.value(c,SkillType.ARCHER)>0&&((ArcherSkill)SkillRegistry.get(SkillType.ARCHER)).headshot(c,victim,arrow)){
                double extra=PowerComposition.damageBonus(c,true,true)-PowerComposition.damageBonus(c,true,false);
                Wire.notice(c,"combat:headshot","headshot","Acerto na cabeça",extra>0?"+"+com.murilloskills.skills.effects.EffectDefinition.number(extra*100)+"% aplicado pelo mod":"Limite de dano já atingido",0,40);
            }
        }
        if(victim instanceof ServerPlayer player){
            var c=context(player);if(source.getEntity()!=null)c.runtime().combatUntil=c.now()+200;
            float before=value;
            for(var skill:SkillRegistry.all())if(c.profile().selected(skill.type()))value=skill.incoming(c,source,value);
            value*=1-SynergyService.bonus(c.profile(),SynergyService.Effect.DEFENSE);
            boolean magma=source.is(DamageTypes.HOT_FLOOR)&&com.murilloskills.impl.ExplorerSkill.NETHER.value(c,SkillType.EXPLORER)>0;
            if(!magma&&!source.is(net.minecraft.tags.DamageTypeTags.IS_FALL))value=(float)PowerComposition.capResistance(before,value,c.store().config.maxSkillResistance);
        }
        return Math.max(0,value);
    }
    public static void refresh(SkillContext c){
        for(var skill:SkillRegistry.all())if(c.profile().selected(skill.type()))skill.updateAttributes(c);else skill.clear(c);
        com.murilloskills.utils.Attributes.set(c,net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,"synergy/speed",SynergyService.bonus(c.profile(),SynergyService.Effect.SPEED),net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        com.murilloskills.utils.Attributes.capMovement(c);
        AchievementService.reconcile(c);
    }
    public static void tick(SkillContext c){
        if(!c.player().isAlive())return;
        for(var skill:SkillRegistry.all())if(c.profile().selected(skill.type())){
            if(c.runtime().activeUntil.containsKey(skill.type())&&!c.active(skill.type())){skill.cancel(c);Wire.notice(c,"ability:"+skill.type().name(),"end",skill.type().ability,"Concluída · recarga em andamento",0,60);refresh(c);Wire.send(c.player(),"");}
            skill.fastTick(c);
        }
        if(!c.runtime().queuedMiningPlan.isEmpty()){
            var plan=c.runtime().queuedMiningPlan;c.runtime().queuedMiningPlan=java.util.List.of();
            AreaTools.mine(c,plan);
        }
        com.murilloskills.utils.ScanService.tick(c);
        com.murilloskills.utils.GrowthService.tick(c);
        for(var pos:java.util.List.copyOf(c.runtime().pendingCollections))if(c.has(SkillType.MINER,15)&&c.player().level().hasChunkAt(pos))AreaTools.collect(c,pos);
        c.runtime().pendingCollections.clear();
        if(c.now()%20==0){
            for(var skill:SkillRegistry.all())if(c.profile().selected(skill.type()))skill.tick(c);
            refresh(c);
            Wire.send(c.player(),"");
        }
    }
}

