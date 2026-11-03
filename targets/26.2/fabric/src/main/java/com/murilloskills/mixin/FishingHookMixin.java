package com.murilloskills.mixin;
import com.murilloskills.*;
import com.murilloskills.api.*;
import com.murilloskills.events.GameplayEvents;
import com.murilloskills.impl.FisherSkill;
import com.murilloskills.skills.SkillType;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@Mixin(FishingHook.class)
abstract class FishingHookMixin {
    @Shadow private int timeUntilLured;
    @Shadow @Final @Mutable private int luck;
    @Unique private int murilloskills$baseLuck;
    @Inject(method="<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;II)V",at=@At("TAIL"))
    private void luck(Player owner,Level world,int originalLuck,int lure,CallbackInfo ci){
        murilloskills$baseLuck=luck;
        if(owner instanceof ServerPlayer p&&MurilloSkills.store!=null){var c=GameplayEvents.context(p);luck+=((FisherSkill)SkillRegistry.get(SkillType.FISHER)).luck(c);}
    }
    @Unique private void murilloskills$refreshLuck(){
        var hook=(FishingHook)(Object)this;
        if(hook.getPlayerOwner() instanceof ServerPlayer p&&MurilloSkills.store!=null)luck=murilloskills$baseLuck+((FisherSkill)SkillRegistry.get(SkillType.FISHER)).luck(GameplayEvents.context(p));
    }
    @Inject(method="tick",at=@At("HEAD"))
    private void refresh(CallbackInfo ci){murilloskills$refreshLuck();}
    @Inject(method="retrieve",at=@At("HEAD"))
    private void current(ItemStack rod,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> ci){murilloskills$refreshLuck();}
    @Inject(method="catchingFish",at=@At("TAIL"))
    private void waitTime(net.minecraft.core.BlockPos pos,CallbackInfo ci){
        var hook=(FishingHook)(Object)this;
        if(hook.getPlayerOwner() instanceof ServerPlayer p&&MurilloSkills.store!=null&&timeUntilLured>0){
            var c=GameplayEvents.context(p);double r=((FisherSkill)SkillRegistry.get(SkillType.FISHER)).waitReduction(c);
            double extra=r/(1-r);int ticks=(int)extra;if(p.getRandom().nextDouble()<extra-ticks)ticks++;
            timeUntilLured=Math.max(1,timeUntilLured-ticks);
        }
    }
    @ModifyExpressionValue(method="retrieve",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"))
    private ObjectArrayList<ItemStack> caught(ObjectArrayList<ItemStack> loot){
        var hook=(FishingHook)(Object)this;
        if(hook.getPlayerOwner() instanceof ServerPlayer p&&MurilloSkills.store!=null){var c=GameplayEvents.context(p);if(c.profile().selected(SkillType.FISHER))((FisherSkill)SkillRegistry.get(SkillType.FISHER)).caught(c,loot);}
        return loot;
    }
    @Inject(method="retrieve",at=@At("RETURN"),cancellable=true)
    private void blessing(ItemStack rod,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> cir){
        var hook=(FishingHook)(Object)this;
        if(cir.getReturnValue()>0&&hook.getPlayerOwner() instanceof ServerPlayer p&&MurilloSkills.store!=null){
            var c=GameplayEvents.context(p);cir.setReturnValue(((FisherSkill)SkillRegistry.get(SkillType.FISHER)).rodDamage(c,cir.getReturnValue()));
        }
    }
}

