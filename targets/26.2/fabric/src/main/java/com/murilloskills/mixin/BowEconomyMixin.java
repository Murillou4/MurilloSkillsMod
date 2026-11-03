package com.murilloskills.mixin;
import com.murilloskills.*;
import com.murilloskills.events.GameplayEvents;
import com.murilloskills.skills.SkillPerk;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.*;
@Mixin(BowItem.class)
abstract class BowEconomyMixin {
    @Inject(method="releaseUsing",at=@At("HEAD"))
    private void before(ItemStack bow,Level level,LivingEntity owner,int remaining,CallbackInfoReturnable<Boolean> cir,@Share("ammo") LocalRef<ItemStack> ammo,@Share("count") LocalIntRef count){
        if(owner instanceof ServerPlayer p&&MurilloSkills.store!=null){var stack=p.getProjectile(bow);ammo.set(stack.copyWithCount(1));count.set(count(p,stack));}
    }
    @Inject(method="releaseUsing",at=@At("RETURN"))
    private void after(ItemStack bow,Level level,LivingEntity owner,int remaining,CallbackInfoReturnable<Boolean> cir,@Share("ammo") LocalRef<ItemStack> ammo,@Share("count") LocalIntRef count){
        if(!cir.getReturnValue()||!(owner instanceof ServerPlayer p)||MurilloSkills.store==null||ammo.get()==null||ammo.get().isEmpty()||p.hasInfiniteMaterials())return;
        var c=GameplayEvents.context(p);
        if(count.get()>count(p,ammo.get())&&p.getRandom().nextDouble()<((com.murilloskills.impl.ArcherSkill)com.murilloskills.api.SkillRegistry.get(com.murilloskills.skills.SkillType.ARCHER)).arrowConservation(c))c.give(ammo.get().copy());
    }
    private static int count(ServerPlayer p,ItemStack item){if(item.isEmpty())return 0;int n=0;for(var stack:p.getInventory().getNonEquipmentItems())if(ItemStack.isSameItemSameComponents(stack,item))n+=stack.getCount();if(ItemStack.isSameItemSameComponents(p.getOffhandItem(),item))n+=p.getOffhandItem().getCount();return n;}
}
